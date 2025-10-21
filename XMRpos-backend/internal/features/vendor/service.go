package vendor

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/rpc"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

type VendorService struct {
	repo      VendorRepository
	db        *gorm.DB
	config    *config.Config
	rpcClient *rpc.Client
	moneroPay *moneropay.MoneroPayAPIClient
	mu        sync.Mutex
}

func NewVendorService(repo VendorRepository, db *gorm.DB, cfg *config.Config, rpcClient *rpc.Client, moneroPay *moneropay.MoneroPayAPIClient) *VendorService {
	return &VendorService{repo: repo, db: db, config: cfg, rpcClient: rpcClient, moneroPay: moneroPay}
}

func (s *VendorService) StartTransferCompleter(ctx context.Context, interval time.Duration) {
	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()
		for {
			select {
			case <-ticker.C:
				// bound each sweep to avoid piling up
				sweepCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
				s.completeTransfers(sweepCtx)
				cancel()
			case <-ctx.Done():
				return
			}
		}
	}()
}

func (s *VendorService) completeTransfers(ctx context.Context) {
	s.mu.Lock()
	defer s.mu.Unlock()

	// We use transfer intead of transfer_split because we need support for subtract_fee_from_outputs
	// We need that because we do not want the server operator to be responsible for covering transaction fees
	// When transfer_split is supported, we can switch to it for a much more efficient transfer process

	// For loop to try and complete transfers
	for i := 15; i > 0; i-- {
		dbTx := s.db.Begin()
		var batchErr error
		func() {
			defer func() {
				if r := recover(); r != nil {
					_ = dbTx.Rollback()
				}
			}()
			// fetch a safe number of transfers to complete
			transfers, err := s.repo.GetTransfersToComplete(ctx, i)
			if err != nil || transfers == nil {
				log.Println("Error fetching transfers to complete:", err)
				batchErr = err
				_ = dbTx.Rollback()
				return
			}

			if len(transfers) == 0 {
				_ = dbTx.Rollback()
				return
			}

			// Mark transactions as transferred
			for _, transfer := range transfers {
				transactionIDs := []uint{}
				for _, tx := range transfer.Transactions {
					transactionIDs = append(transactionIDs, tx.ID)
				}
				if err := s.repo.MarkTransactionsTransferred(ctx, dbTx, transfer.ID, transactionIDs); err != nil {
					log.Printf("Error marking transactions as transferred: %v", err)
					batchErr = err
					_ = dbTx.Rollback()
					return
				}
			}

			destinations := make([]moneropay.Destination, len(transfers))
			for i, transfer := range transfers {
				destinations[i] = moneropay.Destination{
					Amount:  transfer.Amount,
					Address: transfer.Address,
				}
			}

			txHash, amounts, err := s.executeTransfer(ctx, destinations)
			if err != nil {
				log.Printf("Transfer execution failed: %v", err)
				batchErr = err
				_ = dbTx.Rollback()
				return
			}
			if txHash == "" {
				log.Print("Transfer failed, no transaction hash returned")
				batchErr = fmt.Errorf("transfer failed, empty tx hash")
				_ = dbTx.Rollback()
				return
			}
			// We need to mark the transfer as completed

			for index, transfer := range transfers {
				amountTransferred := transfer.Amount
				if len(amounts) > index && amounts[index] != 0 {
					amountTransferred = amounts[index]
				}
				if err := s.repo.MarkTransferCompleted(ctx, dbTx, transfer.ID, amountTransferred, txHash); err != nil {
					log.Println("Error marking transfer as completed:", err)
					batchErr = err
					_ = dbTx.Rollback()
					return
				}
			}
			if err := dbTx.Commit().Error; err != nil {
				log.Println("Error committing transaction:", err)
				batchErr = err
				return
			}
			log.Println("Transfer completed successfully")
			return
		}() // end per-iteration scope
		if batchErr != nil {
			break
		}
	}

}

func (s *VendorService) executeTransfer(ctx context.Context, destinations []moneropay.Destination) (string, []int64, error) {
	if len(destinations) == 0 {
		return "", nil, fmt.Errorf("no destinations provided")
	}

	var rpcErr error
	if s.rpcClient != nil {
		if txHash, amounts, err := s.transferWithWalletRPC(ctx, destinations); err == nil {
			return txHash, amounts, nil
		} else {
			rpcErr = err
			log.Printf("Wallet RPC transfer failed, attempting MoneroPay transfer: %v", err)
		}
	}

	if s.moneroPay != nil {
		txHash, amounts, err := s.transferWithMoneroPay(ctx, destinations)
		if err == nil {
			return txHash, amounts, nil
		}
		if rpcErr != nil {
			return "", nil, fmt.Errorf("wallet RPC transfer failed (%v) and MoneroPay transfer failed (%w)", rpcErr, err)
		}
		return "", nil, err
	}

	if rpcErr != nil {
		return "", nil, fmt.Errorf("wallet RPC transfer failed (%v) and no MoneroPay client configured", rpcErr)
	}

	return "", nil, fmt.Errorf("no transfer backend configured")
}

func (s *VendorService) transferWithWalletRPC(ctx context.Context, destinations []moneropay.Destination) (string, []int64, error) {
	if s.rpcClient == nil {
		return "", nil, fmt.Errorf("wallet RPC client not configured")
	}

	type rpcDestination struct {
		Amount  int64  `json:"amount"`
		Address string `json:"address"`
	}

	type transferParams struct {
		Destinations           []rpcDestination `json:"destinations"`
		SubtractFeeFromOutputs []uint           `json:"subtract_fee_from_outputs,omitempty"`
		DoNotRelay             bool             `json:"do_not_relay,omitempty"`
		Priority               uint             `json:"priority,omitempty"`
	}

	params := transferParams{
		Destinations:           make([]rpcDestination, len(destinations)),
		SubtractFeeFromOutputs: make([]uint, 0, len(destinations)),
		DoNotRelay:             true,
		Priority:               0,
	}

	for i, dest := range destinations {
		params.Destinations[i] = rpcDestination{
			Amount:  dest.Amount,
			Address: dest.Address,
		}
		params.SubtractFeeFromOutputs = append(params.SubtractFeeFromOutputs, uint(i))
	}

	type transferResult struct {
		Amount        int64 `json:"amount"`
		AmountsByDest struct {
			Amounts []int64 `json:"amounts"`
		} `json:"amounts_by_dest"`
		Fee            int64  `json:"fee"`
		MultisigTxset  string `json:"multisig_txset"`
		SpentKeyImages struct {
			KeyImages []string `json:"key_images"`
		} `json:"spent_key_images"`
		TxBlob        string `json:"tx_blob"`
		TxHash        string `json:"tx_hash"`
		TxKey         string `json:"tx_key"`
		TxMetadata    string `json:"tx_metadata"`
		UnsignedTxset string `json:"unsigned_txset"`
		Weight        int64  `json:"weight"`
	}

	// dry-run to ensure the transfer fits in a single transaction
	var dryRun transferResult
	callCtx, cancel := context.WithTimeout(ctx, 10*time.Second)
	err := s.rpcClient.Call(callCtx, "transfer", params, &dryRun)
	cancel()
	if err != nil {
		return "", nil, err
	}

	params.DoNotRelay = false

	var result transferResult
	callCtx2, cancel2 := context.WithTimeout(ctx, 15*time.Second)
	err = s.rpcClient.Call(callCtx2, "transfer", params, &result)
	cancel2()
	if err != nil {
		return "", nil, err
	}

	if result.TxHash == "" {
		return "", nil, fmt.Errorf("wallet RPC transfer returned empty tx hash")
	}

	amounts := result.AmountsByDest.Amounts
	if len(amounts) == 0 {
		amounts = make([]int64, len(destinations))
		for i, dest := range destinations {
			amounts[i] = dest.Amount
		}
	}

	return result.TxHash, amounts, nil
}

func (s *VendorService) transferWithMoneroPay(ctx context.Context, destinations []moneropay.Destination) (string, []int64, error) {
	if s.moneroPay == nil {
		return "", nil, fmt.Errorf("MoneroPay client not configured")
	}

	req := &moneropay.TransferRequest{
		Destinations:           destinations,
		SubtractFeeFromOutputs: make([]uint, len(destinations)),
		DoNotRelay:             false,
		Priority:               0,
	}
	for i := range destinations {
		req.SubtractFeeFromOutputs[i] = uint(i)
	}
	callCtx, cancel := context.WithTimeout(ctx, 15*time.Second)
	defer cancel()

	resp, err := s.moneroPay.PostTransfer(callCtx, req)
	if err != nil {
		return "", nil, err
	}
	if resp == nil {
		return "", nil, fmt.Errorf("MoneroPay transfer returned nil response")
	}

	txHash := resp.TxHash
	if txHash == "" && len(resp.TxHashList) > 0 {
		txHash = resp.TxHashList[0]
	}
	if txHash == "" {
		return "", nil, fmt.Errorf("MoneroPay transfer returned empty tx hash")
	}

	amounts := make([]int64, len(resp.Destinations))
	for i, dest := range resp.Destinations {
		amounts[i] = dest.Amount
	}
	if len(amounts) == 0 {
		amounts = make([]int64, len(destinations))
		for i, dest := range destinations {
			amounts[i] = dest.Amount
		}
	}

	return txHash, amounts, nil
}

func (s *VendorService) CreateVendor(ctx context.Context, name string, password string, inviteCode string) (httpErr *models.HTTPError) {

	if len(name) < 3 || len(name) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.VendorByNameExists(ctx, name)

	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error checking if vendor name exists: "+err.Error())
	}

	if nameTaken {
		return models.NewHTTPError(http.StatusBadRequest, "vendor name already taken")
	}

	invite, err := s.repo.FindInviteByCode(ctx, inviteCode)
	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "invalid invite code")
	}

	if invite.Used {
		return models.NewHTTPError(http.StatusBadRequest, "invite code already used")
	}

	if invite.ForcedName != nil && *invite.ForcedName != name {
		return models.NewHTTPError(http.StatusBadRequest, "invite code is for a different name")
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error hashing password: "+err.Error())
	}

	vendor := &models.Vendor{
		Name:         name,
		PasswordHash: string(hashedPassword),
	}

	err = s.repo.CreateVendor(ctx, vendor)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error creating vendor: "+err.Error())
	}

	err = s.repo.SetInviteToUsed(ctx, invite.ID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error setting invite to used: "+err.Error())
	}

	return nil
}

func (s *VendorService) DeleteVendor(ctx context.Context, vendorID uint) (httpErr *models.HTTPError) {
	vendor, err := s.repo.GetVendorByID(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error retrieving vendor: "+err.Error())
	}

	if vendor == nil {
		return models.NewHTTPError(http.StatusNotFound, "vendor not found")
	}

	if vendor.Balance != 0 {
		return models.NewHTTPError(http.StatusBadRequest, "vendor balance must be 0 to delete vendor")
	}

	err = s.repo.DeleteAllPosForVendor(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting POS for vendor: "+err.Error())
	}

	err = s.repo.DeleteAllTransactionsForVendor(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting transactions for vendor: "+err.Error())
	}

	err = s.repo.DeleteVendor(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting vendor: "+err.Error())
	}

	return nil
}

func (s *VendorService) CreatePos(ctx context.Context, name string, password string, vendorID uint) (httpErr *models.HTTPError) {

	if len(name) < 3 || len(name) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.PosByNameExistsForVendor(ctx, name, vendorID)

	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "error checking if POS name exists: "+err.Error())
	}

	if nameTaken {
		return models.NewHTTPError(http.StatusBadRequest, "POS name already taken")
	}

	// check to see if vendor still exists. This is to prevent POS creation on deleted vendor, but probably needs to be done in a better way
	vendor, err := s.repo.GetVendorByID(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "error retrieving vendor: "+err.Error())
	}

	if vendor == nil {
		return models.NewHTTPError(http.StatusBadRequest, "vendor not found")
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error hashing password: "+err.Error())
	}

	pos := &models.Pos{
		Name:         name,
		PasswordHash: string(hashedPassword),
		VendorID:     vendorID,
	}

	err = s.repo.CreatePos(ctx, pos)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error creating POS: "+err.Error())
	}

	return nil
}

func (s *VendorService) GetBalance(ctx context.Context, vendorID uint) (*int64, *models.HTTPError) {
	balance, err := s.repo.GetBalance(ctx, vendorID)
	if err != nil {
		return nil, models.NewHTTPError(http.StatusInternalServerError, "error retrieving balance: "+err.Error())
	}

	return &balance, nil
}

func (s *VendorService) CreateTransfer(ctx context.Context, vendorID uint, address string) *models.HTTPError {
	s.mu.Lock()
	defer s.mu.Unlock()

	// Check if vendor already has a transfer in progress
	transfer, err := s.repo.GetActiveTransferByVendorID(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "DB error: "+err.Error())
	}
	if transfer != nil {
		return models.NewHTTPError(http.StatusBadRequest, "Transfer already in progress for this vendor")
	}

	/* var transactionIDs []uint */

	transactions, err := s.repo.GetAllTransferableTransactions(ctx, vendorID)

	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "DB error: "+err.Error())
	}

	if len(transactions) == 0 {
		return models.NewHTTPError(http.StatusBadRequest, "No transferable transactions found for this vendor")
	}

	totalAmount := int64(0)

	for _, tx := range transactions {
		totalAmount += tx.Amount
	}

	// Do not allow withdrawals of less than 0.003 XMR as the fee is too high
	if totalAmount < 3000000 {
		return models.NewHTTPError(http.StatusBadRequest, "Minimum transfer amount is 0.003 XMR")
	}

	// Create a new transfer record
	newTransfer := &models.Transfer{
		VendorID:     vendorID,
		Amount:       totalAmount,
		Address:      address,
		Transactions: transactions,
	}

	err = s.repo.CreateTransfer(ctx, newTransfer)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "DB error: "+err.Error())
	}

	return nil
}
