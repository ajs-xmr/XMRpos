package wallet

import (
	"context"
	"log"
	"time"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/rpc"
)

// initialize ensures the configured wallet is opened and auto-refresh is enabled
func Initialize(ctx context.Context, cfg *config.Config, client *rpc.Client) {
	if cfg == nil || client == nil {
		return
	}

	if cfg.MoneroWalletRPCWallet == "" {
		return
	}

	go func() {
		backoff := time.Second
		for {
			if ctx != nil {
				select {
				case <-ctx.Done():
					return
				default:
				}
			}

			// attempt to open wallet file
			openCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
			err := client.Call(openCtx, "open_wallet", map[string]string{
				"filename": cfg.MoneroWalletRPCWallet,
				"password": cfg.MoneroWalletRPCWalletPassword,
			}, nil)
			cancel()
			if err != nil {
				log.Printf("wallet init: open_wallet failed: %v", err)
				time.Sleep(backoff)
				if backoff < 10*time.Second {
					backoff *= 2
				}
				continue
			}

			period := cfg.MoneroWalletAutoRefreshPeriod
			if period <= 0 {
				period = 1
			}

			refreshCtx, cancelRefresh := context.WithTimeout(context.Background(), 5*time.Second)
			err = client.Call(refreshCtx, "auto_refresh", map[string]interface{}{
				"enable": true,
				"period": period,
			}, nil)
			cancelRefresh()
			if err != nil {
				log.Printf("wallet init: auto_refresh failed: %v", err)
				time.Sleep(backoff)
				if backoff < 10*time.Second {
					backoff *= 2
				}
				continue
			}

			log.Printf("wallet init: open_wallet and auto_refresh(period=%d) succeeded", period)
			return
		}
	}()
}
