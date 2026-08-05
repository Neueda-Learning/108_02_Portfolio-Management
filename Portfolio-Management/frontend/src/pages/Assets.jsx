import { usePortfolio } from '../context/PortfolioContext'
import RecommendationPanel from '../components/RecommendationPanel'

export default function Assets() {
  const { portfolioId, loading } = usePortfolio()

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="px-4 py-4 flex flex-col gap-5">
      <div>
        <p className="text-xs uppercase tracking-widest text-slate-500">Rule-Based Analysis</p>
        <h1 className="text-2xl font-bold text-white mt-1">Recommendations</h1>
        <p className="text-sm text-slate-400 mt-1">
          Insights to help balance your portfolio based on your risk profile, goal, and horizon.
        </p>
      </div>

      {portfolioId
        ? <RecommendationPanel portfolioId={portfolioId} />
        : (
          <div className="bg-[#1e293b] rounded-2xl border border-slate-700/50 p-8 text-center">
            <p className="text-slate-500 text-sm">No portfolio loaded yet. Please wait a moment.</p>
          </div>
        )
      }
    </div>
  )
}
