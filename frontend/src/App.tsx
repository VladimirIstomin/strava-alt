import React, { useEffect, useState } from 'react'
import './App.css'

interface UserInfo {
  name: string
  avatar: string
}

function App() {
  const [user, setUser] = useState<UserInfo | null>(null)

  useEffect(() => {
    fetch('http://localhost:8080/api/me', { credentials: 'include' })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data && data.name) {
          setUser({ name: data.name as string, avatar: data.avatar as string })
        }
      })
      .catch(() => {})
  }, [])

  return (
    <>
      <header className="header">
        <div className="logo">StravaAlt</div>
        {user ? (
          <div className="user-block">
            <img src={user.avatar} alt="avatar" className="avatar" />
            <span>{user.name}</span>
            <div className="menu">
              <a href="http://localhost:8080/logout">Logout</a>
            </div>
          </div>
        ) : (
          <a href="http://localhost:8080/login">Login with Strava</a>
        )}
      </header>
      <main className="content">
        <h1>Welcome to StravaAlt</h1>
      </main>
    </>
  )
}

export default App
