import axiosInstance from './axiosInstance';

export const getAssetStats = async (ticker, period = '1M') => {
  const response = await axiosInstance.get(`/market-data/stats/${ticker}`, {
    params: { period }
  });
  return response.data;
};

export const getPriceHistory = async (ticker, startDate, endDate) => {
  const response = await axiosInstance.get(`/market-data/history/${ticker}`, {
    params: { startDate, endDate }
  });
  return response.data;
};

