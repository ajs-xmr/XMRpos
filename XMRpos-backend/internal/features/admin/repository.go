package admin

import (
	"context"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type AdminRepository interface {
	CreateInvite(ctx context.Context, invite *models.Invite) (*models.Invite, error)
	ListVendorsWithBalances(ctx context.Context) ([]VendorSummary, error)
}

type adminRepository struct {
	db *gorm.DB
}

func NewAdminRepository(db *gorm.DB) AdminRepository {
	return &adminRepository{db: db}
}

func (r *adminRepository) CreateInvite(ctx context.Context, invite *models.Invite) (*models.Invite, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	if err := r.db.WithContext(ctx).Create(invite).Error; err != nil {
		return nil, err
	}
	return invite, nil
}

func (r *adminRepository) ListVendorsWithBalances(ctx context.Context) ([]VendorSummary, error) {
	if ctx == nil {
		ctx = context.Background()
	}

	var results []VendorSummary
	err := r.db.WithContext(ctx).
		Model(&models.Vendor{}).
		Select("vendors.id AS id, vendors.name AS name, vendors.monero_subaddress AS monero_subaddress, COALESCE(SUM(CASE WHEN transactions.confirmed = ? AND transactions.transferred = ? THEN transactions.amount ELSE 0 END), 0) AS balance", true, false).
		Joins("LEFT JOIN transactions ON transactions.vendor_id = vendors.id").
		Group("vendors.id, vendors.name, vendors.monero_subaddress").
		Order("vendors.id ASC").
		Scan(&results).Error
	if err != nil {
		return nil, err
	}

	return results, nil
}
