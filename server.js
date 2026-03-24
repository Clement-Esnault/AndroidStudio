const jsonServer = require('json-server')
const axios = require('axios')

const BIN_ID = process.env.JSONBIN_BIN_ID
const API_KEY = process.env.JSONBIN_API_KEY
const JSONBIN_URL = `https://api.jsonbin.io/v3/b/${BIN_ID}`

const headers = {
  'X-Master-Key': API_KEY,
  'Content-Type': 'application/json'
}

// Charger les données depuis JSONBin au démarrage
async function loadData() {
  try {
    const res = await axios.get(JSONBIN_URL + '/latest', { headers })
    return res.data.record
  } catch (e) {
    console.error('Erreur chargement JSONBin:', e.message)
    return { raids: [] }
  }
}

// Sauvegarder les données sur JSONBin
async function saveData(data) {
  try {
    await axios.put(JSONBIN_URL, data, { headers })
    console.log('Données sauvegardées ✅')
  } catch (e) {
    console.error('Erreur sauvegarde JSONBin:', e.message)
  }
}

async function startServer() {
  // Charger les données au démarrage
  const data = await loadData()
  console.log('Données chargées depuis JSONBin ✅')

  const server = jsonServer.create()
  const router = jsonServer.router(data)
  const middlewares = jsonServer.defaults()

  // CORS pour Android
  server.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*')
    res.header('Access-Control-Allow-Headers', '*')
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS')
    if (req.method === 'OPTIONS') return res.sendStatus(200)
    next()
  })

  server.use(middlewares)
  server.use(router)

  // Sauvegarder sur JSONBin après chaque modification
  server.use((req, res, next) => {
    res.on('finish', async () => {
      if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method) && res.statusCode < 300) {
        const currentData = router.db.getState()
        await saveData(currentData)
      }
    })
    next()
  })

  const PORT = process.env.PORT || 3000
  server.listen(PORT, () => {
    console.log(`API démarrée sur le port ${PORT} 🚀`)
  })
}

startServer()
