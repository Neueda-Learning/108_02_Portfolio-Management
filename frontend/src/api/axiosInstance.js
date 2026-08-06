import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

// Normalize error messages from backend GlobalExceptionHandler
api.interceptors.response.use(
  res => res,
  err => {
    const data = err.response?.data
    let message = 'An unexpected error occurred'
    if (data) {
      if (typeof data === 'string') message = data
      else if (data.message) message = data.message
      else message = Object.values(data).join(', ')
    }
    return Promise.reject(new Error(message))
  }
)

export default api

