import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAssetStats, getPriceHistory } from '../api/assetStatsApi';
import { useToast } from '../context/ToastContext';
import {
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart
} from 'recharts';

function toChartRows(history) {
  if (!Array.isArray(history)) return [];
  return history
    .map(item => ({
      date: item?.date ? new Date(item.date).toLocaleDateString() : '-',
      price: Number(item?.close ?? item?.price ?? 0),
      open: Number(item?.open ?? item?.price ?? 0),
      high: Number(item?.high ?? item?.price ?? 0),
      low: Number(item?.low ?? item?.price ?? 0)
    }))
    .filter(row => Number.isFinite(row.price));
}

function buildChartInsights(chartData) {
  const prices = chartData
    .map(point => Number(point?.price))
    .filter(value => Number.isFinite(value))

  if (prices.length === 0) {
    return {
      points: 0,
      average: 0,
      high: 0,
      low: 0,
      returnPct: 0,
      spreadPct: 0,
      trend: 'No data'
    }
  }

  const first = prices[0]
  const last = prices[prices.length - 1]
  const high = Math.max(...prices)
  const low = Math.min(...prices)
  const average = prices.reduce((sum, value) => sum + value, 0) / prices.length
  const returnPct = first !== 0 ? ((last - first) / first) * 100 : 0
  const spreadPct = average !== 0 ? ((high - low) / average) * 100 : 0

  let trend = 'Sideways'
  if (returnPct > 0.5) trend = 'Bullish'
  else if (returnPct < -0.5) trend = 'Bearish'

  return { points: prices.length, average, high, low, returnPct, spreadPct, trend }
}

export default function AssetStatsPage({ tickerOverride = null, isEmbedded = false }) {
  const { ticker: routeTicker } = useParams();
  const today = useMemo(() => new Date().toISOString().split('T')[0], []);
  const effectiveTicker = useMemo(
    () => (tickerOverride || routeTicker || '').toUpperCase(),
    [tickerOverride, routeTicker]
  );

  const navigate = useNavigate();
  const { addToast } = useToast();

  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [selectedPeriod, setSelectedPeriod] = useState('1M');
  const [customRange, setCustomRange] = useState(false);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [chartData, setChartData] = useState([]);
  const [dateError, setDateError] = useState('');
  const chartInsights = useMemo(() => buildChartInsights(chartData), [chartData]);

  useEffect(() => {
    if (!effectiveTicker) {
      setStats(null);
      setChartData([]);
      return;
    }
    const load = async () => {
      try {
        setLoading(true);
        const data = await getAssetStats(effectiveTicker, selectedPeriod);
        setStats(data);
        setChartData(toChartRows(data?.priceHistory));
      } catch (error) {
        addToast('Failed to load asset statistics', 'error');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [effectiveTicker, selectedPeriod, addToast]);

  const loadCustomRange = async () => {
    if (!effectiveTicker) return;
    if (!startDate || !endDate) {
      setDateError('Please select both start and end dates.');
      addToast('Please select both start and end dates', 'error');
      return;
    }

    if (startDate > today || endDate > today) {
      setDateError('Future dates are not allowed. Please choose today or an earlier date.');
      addToast('Future dates are not allowed', 'error');
      return;
    }

    if (startDate > endDate) {
      setDateError('Start date cannot be later than the end date.');
      addToast('Start date must be before end date', 'error');
      return;
    }

    try {
      setDateError('');
      setLoading(true);
      const history = await getPriceHistory(effectiveTicker, startDate, endDate);
      setChartData(toChartRows(history));
      addToast('Custom date range loaded', 'success');
    } catch (error) {
      addToast('Failed to load custom date range', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handlePeriodChange = period => {
    setSelectedPeriod(period);
    setCustomRange(false);
  };

  const toggleCustomRange = () => {
    setCustomRange(prev => !prev);
    setDateError('');
    if (!customRange) {
      const end = new Date();
      const start = new Date();
      start.setMonth(start.getMonth() - 1);
      setEndDate(end.toISOString().split('T')[0]);
      setStartDate(start.toISOString().split('T')[0]);
    }
  };

  if (!effectiveTicker) {
    return <div className="px-4 py-6 text-slate-400">Select an asset to view stats.</div>;
  }

  if (loading && !stats) {
    return <div className="px-4 py-6 text-slate-300">Loading asset statistics...</div>;
  }

  if (!stats) {
    return <div className="px-4 py-6 text-red-400">Failed to load asset data.</div>;
  }

  const currentPrice = Number(stats.currentPrice ?? 0);
  const change = Number(stats.priceChange ?? 0);
  const changePct = Number(stats.priceChangePercent ?? 0);
  const isPriceUp = change >= 0;

  return (
    <div className="px-4 py-4 flex flex-col gap-4">
      {!isEmbedded && (
        <button
          onClick={() => navigate(-1)}
          className="text-blue-400 hover:text-blue-300 text-sm text-left"
        >
          Back
        </button>
      )}

      <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-4">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 className="text-2xl font-bold text-white">{stats.ticker || effectiveTicker}</h2>
            <p className="text-slate-400 text-sm">{stats.name || 'Asset'}</p>
          </div>
          <div className="text-right">
            <p className="text-2xl font-bold text-white">${currentPrice.toFixed(2)}</p>
            <p className={`text-sm ${isPriceUp ? 'text-emerald-400' : 'text-rose-400'}`}>
              {isPriceUp ? '+' : ''}${Math.abs(change).toFixed(2)} ({isPriceUp ? '+' : ''}{changePct.toFixed(2)}%)
            </p>
          </div>
        </div>
      </div>

      <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-4">
        <div className="flex flex-wrap gap-2 mb-4">
          {['1W', '1M', '1Y'].map(period => (
            <button
              key={period}
              onClick={() => handlePeriodChange(period)}
              className={`px-3 py-1.5 rounded-lg text-sm ${
                selectedPeriod === period && !customRange
                  ? 'bg-blue-600 text-white'
                  : 'bg-slate-700 text-slate-200 hover:bg-slate-600'
              }`}
            >
              {period}
            </button>
          ))}
          <button
            onClick={toggleCustomRange}
            className={`px-3 py-1.5 rounded-lg text-sm ${
              customRange
                ? 'bg-purple-600 text-white'
                : 'bg-slate-700 text-slate-200 hover:bg-slate-600'
            }`}
          >
            Custom Range
          </button>
        </div>

        {customRange && (
          <div className="space-y-3 mb-4">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              <input
                type="date"
                value={startDate}
                max={today}
                onChange={e => { setStartDate(e.target.value); setDateError('') }}
                className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-slate-200"
              />
              <input
                type="date"
                value={endDate}
                max={today}
                onChange={e => { setEndDate(e.target.value); setDateError('') }}
                className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-slate-200"
              />
              <button
                onClick={loadCustomRange}
                disabled={!startDate || !endDate || startDate > today || endDate > today || startDate > endDate}
                className="bg-blue-600 hover:bg-blue-500 rounded-lg px-3 py-2 text-white disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Apply
              </button>
            </div>

            {dateError && (
              <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-rose-200 text-sm shadow-sm">
                <div className="flex items-start gap-2">
                  <span className="text-rose-300 text-base leading-none">⚠</span>
                  <div>
                    <p className="font-semibold">Custom date range error</p>
                    <p className="text-rose-100/90 mt-0.5">{dateError}</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        <div className="w-full h-[320px]">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData}>
              <defs>
                <linearGradient id="statsPrice" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.05} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="date" tick={{ fill: '#94a3b8', fontSize: 11 }} />
              <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} tickFormatter={v => `$${v.toFixed(2)}`} />
              <Tooltip formatter={value => [`$${Number(value).toFixed(2)}`, 'Price']} />
              <Area
                type="monotone"
                dataKey="price"
                name="Close"
                stroke="#60a5fa"
                fill="url(#statsPrice)"
                fillOpacity={1}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="mt-4">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
              Quick Insights
            </p>
            <p className="text-[11px] text-slate-500">
              {customRange ? 'Custom Range' : selectedPeriod} · {chartInsights.points} points
            </p>
          </div>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-2">
            <InsightCard
              label="Period Return"
              value={`${chartInsights.returnPct >= 0 ? '+' : ''}${chartInsights.returnPct.toFixed(2)}%`}
              accent={chartInsights.returnPct >= 0 ? 'text-emerald-400' : 'text-rose-400'}
              hint={chartInsights.returnPct >= 0 ? 'Up over visible range' : 'Down over visible range'}
            />
            <InsightCard
              label="Range"
              value={`$${chartInsights.low.toFixed(2)} - $${chartInsights.high.toFixed(2)}`}
              hint={`${chartInsights.spreadPct.toFixed(2)}% spread`}
            />
            <InsightCard
              label="Average Close"
              value={`$${chartInsights.average.toFixed(2)}`}
              hint="Mean visible close price"
            />
            <InsightCard
              label="Trend"
              value={chartInsights.trend}
              accent={chartInsights.trend === 'Bullish' ? 'text-emerald-400' : chartInsights.trend === 'Bearish' ? 'text-rose-400' : 'text-amber-300'}
              hint="Based on first vs last point"
            />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-3">
          <p className="text-xs text-slate-400">Week High</p>
          <p className="text-slate-100 font-semibold">${Number(stats.weekHigh ?? 0).toFixed(2)}</p>
        </div>
        <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-3">
          <p className="text-xs text-slate-400">Week Low</p>
          <p className="text-slate-100 font-semibold">${Number(stats.weekLow ?? 0).toFixed(2)}</p>
        </div>
        <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-3">
          <p className="text-xs text-slate-400">Month High</p>
          <p className="text-slate-100 font-semibold">${Number(stats.monthHigh ?? 0).toFixed(2)}</p>
        </div>
        <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-3">
          <p className="text-xs text-slate-400">Month Low</p>
          <p className="text-slate-100 font-semibold">${Number(stats.monthLow ?? 0).toFixed(2)}</p>
        </div>
      </div>
    </div>
  );
}

function InsightCard({ label, value, hint, accent = 'text-slate-100' }) {
  return (
    <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-3">
      <p className="text-[11px] uppercase tracking-wide text-slate-500 mb-1">{label}</p>
      <p className={`text-sm font-bold ${accent}`}>{value}</p>
      {hint && <p className="text-[11px] text-slate-500 mt-1">{hint}</p>}
    </div>
  )
}
