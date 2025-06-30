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
  start_date: string
  type: string
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
  const [recent, setRecent] = useState<Activity[]>([])
  const [view, setView] = useState<'home' | 'activities'>('home')
  const [activities, setActivities] = useState<Activity[]>([])
  const [offset, setOffset] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetch(`${BACKEND_URL}/api/me`, { credentials: 'include' })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data && data.name) {
          setUser({ name: data.name as string, avatar: data.avatar as string })
        }
      })
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (!user) return
    fetch(`${BACKEND_URL}/api/activities?limit=5&offset=0`, { credentials: 'include' })
      .then(res => res.ok ? res.json() : [])
      .then(data => setRecent(data as Activity[]))
      .catch(() => {})
  }, [user])

  useEffect(() => {
    if (view !== 'activities' || loading || !hasMore) return
    setLoading(true)
    fetch(`${BACKEND_URL}/api/activities?limit=20&offset=${offset}`, { credentials: 'include' })
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
        <div className="logo">StravaAlt</div>
        {user ? (
          <div className="user-block">
            <img src={user.avatar} alt="avatar" className="avatar" />
            <span>{user.name}</span>
            <div className="menu">
              <a href={`${BACKEND_URL}/logout`}>Logout</a>
            </div>
          </div>
        ) : (
          <a href={`${BACKEND_URL}/login`}>Login with Strava</a>
        )}
      </header>
      <main className="content">
        {view === 'home' && (
          <>
            <h1>Welcome to StravaAlt</h1>
            {user && (
              <>
                <h2>Последние активности</h2>
                <ul className="activity-list">
                  {recent.map(a => (
                    <li key={a.id} className="activity-item">
                      <span>{emojiMap[a.type] || '❓'} {a.name}</span>
                      <span>{new Date(a.start_date).toLocaleDateString()}</span>
                    </li>
                  ))}
                </ul>
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
              </>
            )}
          </>
        )}
        {view === 'activities' && (
          <>
            <button onClick={() => setView('home')} className="back-button">На главную</button>
            <ul className="activity-list">
              {activities.map(a => (
                <li key={a.id} className="activity-item">
                  <span>{emojiMap[a.type] || '❓'} {a.name}</span>
                  <span>{new Date(a.start_date).toLocaleDateString()}</span>
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
