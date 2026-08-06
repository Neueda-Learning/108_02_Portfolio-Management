import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer
} from 'recharts'

function CustomTooltip({ active, payload }) {
  if (active && payload?.length) {
    return (
      <div className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-xs">
        <p className="text-blue-400 font-semibold">
          ${Number(payload[0].value).toLocaleString('en-US', { minimumFractionDigits: 2 })}
        </p>
        <p className="text-slate-400">{payload[0].payload.time}</p>
      </div>
    )
  }
  return null
}

export default function PortfolioValueGraph({ data }) {
  if (!data || data.length < 2) {
    return (
      <div className="h-36 flex items-center justify-center text-slate-500 text-sm">
        Collecting live data…
      </div>
    )
  }

  return (
    <div className="h-36 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="valueGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%"  stopColor="#3b82f6" stopOpacity={0.3} />
              <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}   />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={false} />
          <XAxis
            dataKey="time"
            tick={{ fill: '#64748b', fontSize: 10 }}
            tickLine={false}
            axisLine={false}
            interval="preserveStartEnd"
          />
          <YAxis
            tick={{ fill: '#64748b', fontSize: 10 }}
            tickLine={false}
            axisLine={false}
            tickFormatter={v => `$${(v / 1000).toFixed(0)}k`}
          />
          <Tooltip content={<CustomTooltip />} />
          <Area
            type="monotone"
            dataKey="value"
            stroke="#3b82f6"
            strokeWidth={2}
            fill="url(#valueGradient)"
            dot={false}
            activeDot={{ r: 4, fill: '#3b82f6' }}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}

