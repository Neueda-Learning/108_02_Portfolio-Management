-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    wallet_balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00
);

SET @wallet_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'wallet_balance'
);
SET @wallet_col_sql = IF(
    @wallet_col_exists = 0,
    'ALTER TABLE users ADD COLUMN wallet_balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00',
    'SELECT 1'
);
PREPARE wallet_col_stmt FROM @wallet_col_sql;
EXECUTE wallet_col_stmt;
DEALLOCATE PREPARE wallet_col_stmt;

-- Insert initial users
INSERT INTO users (user_id, username, wallet_balance)
SELECT 1, 'Sailu', 10000.00
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 1);

INSERT INTO users (user_id, username, wallet_balance)
SELECT 2, 'Sammed', 5000.00
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 2);

INSERT INTO users (user_id, username, wallet_balance)
SELECT 3, 'Anikait', 7500.00
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 3);

INSERT INTO users (user_id, username, wallet_balance)
SELECT 4, 'Aryan', 3000.00
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 4);

-- Wallet transaction history table
CREATE TABLE IF NOT EXISTS wallet_transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    balance_before DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Portfolios table with user_id foreign key
CREATE TABLE IF NOT EXISTS portfolios (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    portfolio_number BIGINT   NOT NULL,
    name       VARCHAR(255)  NOT NULL,
    description VARCHAR(1000),
    currency   VARCHAR(10)   DEFAULT 'USD',
    risk_level VARCHAR(50),
    investment_goal VARCHAR(50),
    target_value DECIMAL(19, 4),
    investment_horizon VARCHAR(50),
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


SET @portfolio_user_id_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND column_name = 'user_id'
);
SET @portfolio_user_id_col_sql = IF(
    @portfolio_user_id_col_exists = 0,
    'ALTER TABLE portfolios ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1',
    'SELECT 1'
);
PREPARE portfolio_user_id_col_stmt FROM @portfolio_user_id_col_sql;
EXECUTE portfolio_user_id_col_stmt;
DEALLOCATE PREPARE portfolio_user_id_col_stmt;

SET @portfolio_number_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND column_name = 'portfolio_number'
);
SET @portfolio_number_col_sql = IF(
    @portfolio_number_col_exists = 0,
    'ALTER TABLE portfolios ADD COLUMN portfolio_number BIGINT',
    'SELECT 1'
);
PREPARE portfolio_number_col_stmt FROM @portfolio_number_col_sql;
EXECUTE portfolio_number_col_stmt;
DEALLOCATE PREPARE portfolio_number_col_stmt;

-- Backfill missing numbers as 1..N per user (ordered by id) for legacy rows.
UPDATE portfolios p
JOIN (
    SELECT ranked_rows.id,
           ranked_rows.seq
    FROM (
        SELECT t.id,
               t.user_id,
               (@row_num := IF(@current_user = t.user_id, @row_num + 1, 1)) AS seq,
               (@current_user := t.user_id) AS tracker
        FROM portfolios t
        CROSS JOIN (SELECT @current_user := NULL, @row_num := 0) vars
        ORDER BY t.user_id, t.id
    ) ranked_rows
) ranked ON ranked.id = p.id
SET p.portfolio_number = ranked.seq
WHERE p.portfolio_number IS NULL OR p.portfolio_number <= 0;

SET @portfolio_number_nullable = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND column_name = 'portfolio_number'
      AND is_nullable = 'YES'
);
SET @portfolio_number_not_null_sql = IF(
    @portfolio_number_nullable > 0,
    'ALTER TABLE portfolios MODIFY COLUMN portfolio_number BIGINT NOT NULL',
    'SELECT 1'
);
PREPARE portfolio_number_not_null_stmt FROM @portfolio_number_not_null_sql;
EXECUTE portfolio_number_not_null_stmt;
DEALLOCATE PREPARE portfolio_number_not_null_stmt;

SET @portfolio_user_number_uk_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND index_name = 'uk_portfolios_user_number'
);
SET @portfolio_user_number_uk_sql = IF(
    @portfolio_user_number_uk_exists = 0,
    'ALTER TABLE portfolios ADD CONSTRAINT uk_portfolios_user_number UNIQUE (user_id, portfolio_number)',
    'SELECT 1'
);
PREPARE portfolio_user_number_uk_stmt FROM @portfolio_user_number_uk_sql;
EXECUTE portfolio_user_number_uk_stmt;
DEALLOCATE PREPARE portfolio_user_number_uk_stmt;

SET @portfolio_currency_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND column_name = 'currency'
);
SET @portfolio_currency_col_sql = IF(
    @portfolio_currency_col_exists = 0,
    'ALTER TABLE portfolios ADD COLUMN currency VARCHAR(10) DEFAULT ''USD''',
    'SELECT 1'
);
PREPARE portfolio_currency_col_stmt FROM @portfolio_currency_col_sql;
EXECUTE portfolio_currency_col_stmt;
DEALLOCATE PREPARE portfolio_currency_col_stmt;

SET @portfolio_risk_level_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND column_name = 'risk_level'
);
SET @portfolio_risk_level_col_sql = IF(
    @portfolio_risk_level_col_exists = 0,
    'ALTER TABLE portfolios ADD COLUMN risk_level VARCHAR(50)',
    'SELECT 1'
);
PREPARE portfolio_risk_level_col_stmt FROM @portfolio_risk_level_col_sql;
EXECUTE portfolio_risk_level_col_stmt;
DEALLOCATE PREPARE portfolio_risk_level_col_stmt;

SET @portfolio_investment_goal_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND column_name = 'investment_goal'
);
SET @portfolio_investment_goal_col_sql = IF(
    @portfolio_investment_goal_col_exists = 0,
    'ALTER TABLE portfolios ADD COLUMN investment_goal VARCHAR(50)',
    'SELECT 1'
);
PREPARE portfolio_investment_goal_col_stmt FROM @portfolio_investment_goal_col_sql;
EXECUTE portfolio_investment_goal_col_stmt;
DEALLOCATE PREPARE portfolio_investment_goal_col_stmt;

SET @portfolio_target_value_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND column_name = 'target_value'
);
SET @portfolio_target_value_col_sql = IF(
    @portfolio_target_value_col_exists = 0,
    'ALTER TABLE portfolios ADD COLUMN target_value DECIMAL(19, 4)',
    'SELECT 1'
);
PREPARE portfolio_target_value_col_stmt FROM @portfolio_target_value_col_sql;
EXECUTE portfolio_target_value_col_stmt;
DEALLOCATE PREPARE portfolio_target_value_col_stmt;

SET @portfolio_investment_horizon_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolios'
      AND column_name = 'investment_horizon'
);
SET @portfolio_investment_horizon_col_sql = IF(
    @portfolio_investment_horizon_col_exists = 0,
    'ALTER TABLE portfolios ADD COLUMN investment_horizon VARCHAR(50)',
    'SELECT 1'
);
PREPARE portfolio_investment_horizon_col_stmt FROM @portfolio_investment_horizon_col_sql;
EXECUTE portfolio_investment_horizon_col_stmt;
DEALLOCATE PREPARE portfolio_investment_horizon_col_stmt;


-- Insert seed portfolios so demo/frontend portfolio id 1 always exists
-- RiskLevel:       CONSERVATIVE | MODERATE | AGGRESSIVE | SPECULATIVE
-- InvestmentGoal:  GROWTH | INCOME | CAPITAL_PRESERVATION | BALANCED | SPECULATION
-- InvestmentHorizon: SHORT_TERM | MEDIUM_TERM | LONG_TERM
INSERT INTO portfolios (id, user_id, portfolio_number, name, description, currency, risk_level, investment_goal, target_value, investment_horizon, created_at, updated_at)
SELECT 1, 1, 1, 'Tech Portfolio', 'Portfolio focused on tech stocks', 'USD', 'AGGRESSIVE', 'GROWTH', 50000.00, 'LONG_TERM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM portfolios WHERE id = 1);

INSERT INTO portfolios (id, user_id, portfolio_number, name, description, currency, risk_level, investment_goal, target_value, investment_horizon, created_at, updated_at)
SELECT 2, 1, 2, 'Blue Chip Portfolio', 'Conservative blue chip stocks', 'USD', 'CONSERVATIVE', 'INCOME', 30000.00, 'LONG_TERM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM portfolios WHERE id = 2);

INSERT INTO portfolios (id, user_id, portfolio_number, name, description, currency, risk_level, investment_goal, target_value, investment_horizon, created_at, updated_at)
SELECT 3, 2, 1, 'Mixed Portfolio', 'Balanced mix of stocks and bonds', 'USD', 'MODERATE', 'BALANCED', 40000.00, 'MEDIUM_TERM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM portfolios WHERE id = 3);

INSERT INTO portfolios (id, user_id, portfolio_number, name, description, currency, risk_level, investment_goal, target_value, investment_horizon, created_at, updated_at)
SELECT 4, 3, 1, 'Crypto Portfolio', 'Alternative investments and crypto', 'USD', 'SPECULATIVE', 'GROWTH', 20000.00, 'SHORT_TERM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM portfolios WHERE id = 4);

INSERT INTO portfolios (id, user_id, portfolio_number, name, description, currency, risk_level, investment_goal, target_value, investment_horizon, created_at, updated_at)
SELECT 5, 4, 1, 'Dividend Portfolio', 'High dividend yielding stocks', 'USD', 'MODERATE', 'INCOME', 25000.00, 'LONG_TERM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM portfolios WHERE id = 5);

-- Fix ownership for legacy rows where user_id was defaulted to 1
UPDATE portfolios SET user_id = 1 WHERE name = 'Tech Portfolio';
UPDATE portfolios SET user_id = 1 WHERE name = 'Blue Chip Portfolio';
UPDATE portfolios SET user_id = 2 WHERE name = 'Mixed Portfolio';
UPDATE portfolios SET user_id = 3 WHERE name = 'Crypto Portfolio';
UPDATE portfolios SET user_id = 4 WHERE name = 'Dividend Portfolio';
UPDATE portfolios SET portfolio_number = 1 WHERE id = 1;
UPDATE portfolios SET portfolio_number = 2 WHERE id = 2;
UPDATE portfolios SET portfolio_number = 1 WHERE id = 3;
UPDATE portfolios SET portfolio_number = 1 WHERE id = 4;
UPDATE portfolios SET portfolio_number = 1 WHERE id = 5;

-- Fix any existing rows that have old/wrong enum values
UPDATE portfolios SET risk_level = 'AGGRESSIVE',   investment_horizon = 'LONG_TERM'   WHERE id = 1 AND (risk_level NOT IN ('CONSERVATIVE','MODERATE','AGGRESSIVE','SPECULATIVE') OR investment_horizon NOT IN ('SHORT_TERM','MEDIUM_TERM','LONG_TERM'));
UPDATE portfolios SET risk_level = 'CONSERVATIVE',  investment_horizon = 'LONG_TERM'   WHERE id = 2 AND (risk_level NOT IN ('CONSERVATIVE','MODERATE','AGGRESSIVE','SPECULATIVE') OR investment_horizon NOT IN ('SHORT_TERM','MEDIUM_TERM','LONG_TERM'));
UPDATE portfolios SET risk_level = 'MODERATE',      investment_horizon = 'MEDIUM_TERM' WHERE id = 3 AND (risk_level NOT IN ('CONSERVATIVE','MODERATE','AGGRESSIVE','SPECULATIVE') OR investment_horizon NOT IN ('SHORT_TERM','MEDIUM_TERM','LONG_TERM'));
UPDATE portfolios SET risk_level = 'SPECULATIVE',   investment_horizon = 'SHORT_TERM'  WHERE id = 4 AND (risk_level NOT IN ('CONSERVATIVE','MODERATE','AGGRESSIVE','SPECULATIVE') OR investment_horizon NOT IN ('SHORT_TERM','MEDIUM_TERM','LONG_TERM'));
UPDATE portfolios SET risk_level = 'MODERATE',      investment_horizon = 'LONG_TERM'   WHERE id = 5 AND (risk_level NOT IN ('CONSERVATIVE','MODERATE','AGGRESSIVE','SPECULATIVE') OR investment_horizon NOT IN ('SHORT_TERM','MEDIUM_TERM','LONG_TERM'));



-- Portfolio items table
CREATE TABLE IF NOT EXISTS portfolio_items (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id   BIGINT         NOT NULL,
    asset_type     VARCHAR(50)    NOT NULL,
    symbol         VARCHAR(255)   NOT NULL,
    name           VARCHAR(255)   NOT NULL,
    quantity       DECIMAL(19, 4) NOT NULL,
    purchase_price DECIMAL(19, 2) NOT NULL,
    current_price  DECIMAL(19, 2),
    purchase_date  TIMESTAMP      NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP,
    notes          VARCHAR(1000),
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
);


-- Insert sample portfolio items for demo portfolios
INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 1, 1, 'STOCK', 'AAPL', 'Apple Inc.', 10.0000, 150.00, 185.50, '2023-01-15 10:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Tech giant, strong growth'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 1);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 2, 1, 'STOCK', 'MSFT', 'Microsoft Corporation', 5.0000, 250.00, 380.00, '2023-02-20 14:30:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Cloud computing leader'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 2);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 3, 1, 'STOCK', 'GOOGL', 'Alphabet Inc.', 3.0000, 100.00, 140.00, '2023-03-10 09:15:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'AI and search leader'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 3);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 4, 2, 'STOCK', 'JNJ', 'Johnson & Johnson', 8.0000, 145.00, 165.00, '2023-04-05 11:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Dividend paying blue chip'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 4);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 5, 2, 'STOCK', 'KO', 'Coca-Cola Company', 12.0000, 60.00, 68.50, '2023-05-12 13:45:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Stable dividend stock'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 5);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 6, 3, 'BOND', 'TLT', 'iShares 20+ Year Treasury Bond', 20.0000, 95.00, 88.75, '2023-06-01 10:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Long-term bonds'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 6);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 7, 3, 'ETF', 'VOO', 'Vanguard S&P 500 ETF', 5.0000, 350.00, 465.00, '2023-07-15 15:20:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Broad market exposure'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 7);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 8, 4, 'CRYPTO', 'BTC', 'Bitcoin', 0.5000, 30000.00, 45000.00, '2023-08-22 12:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Digital gold'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 8);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 9, 4, 'CRYPTO', 'ETH', 'Ethereum', 2.0000, 1800.00, 2500.00, '2023-09-30 14:30:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Smart contract platform'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 9);

INSERT INTO portfolio_items (id, portfolio_id, asset_type, symbol, name, quantity, purchase_price, current_price, purchase_date, created_at, updated_at, notes)
SELECT 10, 5, 'STOCK', 'PG', 'Procter & Gamble', 15.0000, 130.00, 155.00, '2023-10-14 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Consumer staples dividend'
WHERE NOT EXISTS (SELECT 1 FROM portfolio_items WHERE id = 10);


