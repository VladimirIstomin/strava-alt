import React, { useEffect, useState } from 'react'

function App() {
  const [name, setName] = useState<string | null>(null)

  useEffect(() => {
    fetch('http://localhost:8080/api/me', { credentials: 'include' })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data && data.name) {
          setName(data.name as string)
        }
      })
      .catch(() => {})
  }, [])

  return (
    <>
      <header style={{ display: 'flex', justifyContent: 'flex-end', padding: '1rem' }}>
        {name ? (
          <span>{name}</span>
        ) : (
          <a href="http://localhost:8080/login">Login with Strava</a>
        )}
      </header>
      <h1>StravaAlt Frontend</h1>
    </>
  )
}

export default App
