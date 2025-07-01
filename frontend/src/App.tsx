import React, { useEffect, useState } from 'react'
import './App.css'

const BACKEND_URL = import.meta.env.BACKEND_URL || 'http://localhost:8080'

interface UserInfo {
  name: string
  avatar: string
}

interface Activity {
  id: number
  name: string
  startDate: string
  type: string
  averageHeartrate?: number
  averageSpeed?: number
  movingTime?: number
  distance?: number
  averageCadence?: number
}

const emojiMap: Record<string, string> = {
  Run: '🏃',
  Ride: '🚴',
  Swim: '🏊',
  Walk: '🚶',
  Hike: '🥾',
  Workout: '🏋️',
}

function App() {
  const [user, setUser] = useState<UserInfo | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [recent, setRecent] = useState<Activity[]>([])
  const [view, setView] = useState<'home' | 'activities'>('home')
  const [activities, setActivities] = useState<Activity[]>([])
  const [offset, setOffset] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [expandedId, setExpandedId] = useState<number | null>(null)

  // Restore the current view from the URL hash on initial load
  useEffect(() => {
    const hash = window.location.hash.replace('#', '')
    if (hash === 'activities') {
      setView('activities')
    } else {
      setView('home')
    }
  }, [])

  // Persist the view state in the URL hash so refreshes keep the same screen
  useEffect(() => {
    if (view === 'activities') {
      window.location.hash = 'activities'
    } else {
      window.location.hash = ''
    }
  }, [view])

  const formatTime = (seconds?: number): string => {
    if (seconds == null) return '—'
    const h = Math.floor(seconds / 3600)
    const m = Math.floor((seconds % 3600) / 60)
    const s = seconds % 60
    return [h, m, s]
      .map(v => v.toString().padStart(2, '0'))
      .join(':')
  }

  useEffect(() => {
    const url = new URL(window.location.href)
    const tokenParam = url.searchParams.get('access_token')
    if (tokenParam) {
      localStorage.setItem('accessToken', tokenParam)
      setAccessToken(tokenParam)
      url.searchParams.delete('access_token')
      window.history.replaceState(null, '', url.toString())
    } else {
      const stored = localStorage.getItem('accessToken')
      if (stored) setAccessToken(stored)
    }
  }, [])

  const refresh = async (): Promise<string | null> => {
    const res = await fetch(`${BACKEND_URL}/api/v1/refresh`, {
      method: 'POST',
      credentials: 'include'
    })
    if (!res.ok) return null
    const data = await res.json()
    if (data.access_token) {
      localStorage.setItem('accessToken', data.access_token)
      setAccessToken(data.access_token)
      return data.access_token as string
    }
    return null
  }

  const fetchWithAuth = async (path: string, options: RequestInit = {}): Promise<Response> => {
    const token = accessToken
    const headers = { ...(options.headers || {}), ...(token ? { 'Authorization': `Bearer ${token}` } : {}) }
    let res = await fetch(`${BACKEND_URL}${path}`, { ...options, headers })
    if (res.status === 401) {
      const newToken = await refresh()
      if (newToken) {
        const retryHeaders = { ...(options.headers || {}), 'Authorization': `Bearer ${newToken}` }
        res = await fetch(`${BACKEND_URL}${path}`, { ...options, headers: retryHeaders })
      }
    }
    return res
  }

  useEffect(() => {
    if (!accessToken) return
    fetchWithAuth('/api/v1/me')
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data && data.name) {
          setUser({ name: data.name as string, avatar: data.avatar as string })
        }
      })
      .catch(() => {})
  }, [accessToken])

  useEffect(() => {
    if (!user) return
    fetchWithAuth(`/api/v1/activities?limit=5&offset=0`)
      .then(res => res.ok ? res.json() : [])
      .then(data => setRecent(data as Activity[]))
      .catch(() => {})
  }, [user])

  useEffect(() => {
    if (view !== 'activities' || loading || !hasMore) return
    setLoading(true)
    fetchWithAuth(`/api/v1/activities?limit=20&offset=${offset}`)
      .then(res => res.ok ? res.json() : [])
      .then((data: Activity[]) => {
        setActivities(prev => [...prev, ...data])
        if (data.length === 0) setHasMore(false)
      })
      .catch(() => setHasMore(false))
      .finally(() => setLoading(false))
  }, [offset, view])

  useEffect(() => {
    if (view !== 'activities') return
    const onScroll = () => {
      if (!hasMore || loading) return
      if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 100) {
        setOffset(o => o + 20)
      }
    }
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [view, hasMore, loading])

  return (
    <>
      <header className="header">
        <div className="logo">Travalt</div>
        {user ? (
          <div className="user-block">
            <img src={user.avatar} alt="avatar" className="avatar" />
            <span>{user.name}</span>
            <div className="menu">
              <a
                href={`${BACKEND_URL}/api/v1/logout`}
                onClick={() => localStorage.removeItem('accessToken')}
              >
                Logout
              </a>
            </div>
          </div>
        ) : (
          <a href={`${BACKEND_URL}/api/v1/login`}>Login with Strava</a>
        )}
      </header>
      <main className="content">
        {view === 'home' && (
          <>
            {!user && <h1>Welcome to Travalt</h1>}
            {user && (
              <>
              <div className="activities-header">
                <h2>Последние активности</h2>
                <button
                  onClick={() => {
                    setActivities([])
                    setOffset(0)
                    setHasMore(true)
                    setView('activities')
                  }}
                  className="all-button"
                >
                  Все
                </button>
              </div>
              <ul className="activity-list">
                {recent.map(a => (
                  <li key={a.id} className="activity-item">
                    <button
                      className="activity-header"
                      onClick={() =>
                        setExpandedId(expandedId === a.id ? null : a.id)
                      }
                    >
                      <span>
                        {emojiMap[a.type] || '❓'} {a.name}
                      </span>
                      <span>
                        {new Date(a.startDate).toLocaleString()}
                      </span>
                    </button>
                    {expandedId === a.id && (
                      <div className="activity-details">
                        <div>
                          ❤️ Средний пульс:{' '}
                          {a.averageHeartrate?.toFixed(0) || '—'}
                        </div>
                        <div>
                          💨 Средняя скорость:{' '}
                          {a.averageSpeed
                            ? (a.averageSpeed * 3.6).toFixed(1)
                            : '—'}{' '}
                          км/ч
                        </div>
                        <div>⏱ Время: {formatTime(a.movingTime)}</div>
                        <div>
                          🛣 Дистанция:{' '}
                          {a.distance
                            ? (a.distance / 1000).toFixed(2)
                            : '—'}{' '}
                          км
                        </div>
                        {a.type === 'Ride' && (
                          <div>
                            🔄 Каденс:{' '}
                            {a.averageCadence?.toFixed(0) || '—'}
                          </div>
                        )}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
              </>
            )}
          </>
        )}
        {view === 'activities' && (
          <>
            <div className="activities-header">
              <h2>Все активности</h2>
              <button onClick={() => setView('home')} className="back-button">Назад</button>
            </div>
            <ul className="activity-list">
              {activities.map(a => (
                <li key={a.id} className="activity-item">
                  <button
                    className="activity-header"
                    onClick={() =>
                      setExpandedId(expandedId === a.id ? null : a.id)
                    }
                  >
                    <span>
                      {emojiMap[a.type] || '❓'} {a.name}
                    </span>
                    <span>
                      {new Date(a.startDate).toLocaleString()}
                    </span>
                  </button>
                  {expandedId === a.id && (
                    <div className="activity-details">
                      <div>
                        ❤️ Средний пульс:{' '}
                        {a.averageHeartrate?.toFixed(0) || '—'}
                      </div>
                      <div>
                        💨 Средняя скорость:{' '}
                        {a.averageSpeed
                          ? (a.averageSpeed * 3.6).toFixed(1)
                          : '—'}{' '}
                        км/ч
                      </div>
                      <div>⏱ Время: {formatTime(a.movingTime)}</div>
                      <div>
                        🛣 Дистанция:{' '}
                        {a.distance
                          ? (a.distance / 1000).toFixed(2)
                          : '—'}{' '}
                        км
                      </div>
                      {a.type === 'Ride' && (
                        <div>
                          🔄 Каденс:{' '}
                          {a.averageCadence?.toFixed(0) || '—'}
                        </div>
                      )}
                    </div>
                  )}
                </li>
              ))}
            </ul>
            {loading && <p>Загрузка...</p>}
            {!hasMore && <p>Это все активности</p>}
          </>
        )}
      </main>
    </>
  )
}

export default App
