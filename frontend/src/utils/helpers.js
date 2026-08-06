// Shared helpers used across components

export const ASSET_TYPES = ['STOCK', 'ETF', 'MUTUAL_FUND', 'BOND', 'CRYPTO', 'CASH', 'OTHER']

export const ASSET_LABELS = {
  STOCK:       'Stocks',
  BOND:        'Bonds',
  ETF:         'ETFs',
  MUTUAL_FUND: 'Mutual Funds',
  CRYPTO:      'Crypto',
  CASH:        'Cash',
  OTHER:       'Other'
}

export const ASSET_ICONS = {
  STOCK:       '📈',
  BOND:        '📄',
  ETF:         '🏦',
  MUTUAL_FUND: '💼',
  CRYPTO:      '₿',
  CASH:        '💵',
  OTHER:       '🔶'
}

export const ASSET_COLORS = {
  STOCK:       'bg-blue-500/20 text-blue-300',
  BOND:        'bg-yellow-500/20 text-yellow-300',
  ETF:         'bg-purple-500/20 text-purple-300',
  MUTUAL_FUND: 'bg-orange-500/20 text-orange-300',
  CRYPTO:      'bg-cyan-500/20 text-cyan-300',
  CASH:        'bg-green-500/20 text-green-300',
  OTHER:       'bg-slate-500/20 text-slate-300'
}

export const fmt = {
  currency: (v) =>
    Number(v).toLocaleString('en-US', { style: 'currency', currency: 'USD' }),
  percent: (v) =>
    `${Number(v) >= 0 ? '+' : ''}${Number(v).toFixed(2)}%`,
  number: (v) =>
    Number(v).toLocaleString('en-US', { maximumFractionDigits: 4 })
}

export const profitClass = (v) =>
  Number(v) >= 0 ? 'text-green-400' : 'text-red-400'

