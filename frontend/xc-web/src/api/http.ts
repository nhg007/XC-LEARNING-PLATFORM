const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`)
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }
  return response.json() as Promise<T>
}
