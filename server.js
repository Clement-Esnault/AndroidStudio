const express = require('express')
const axios = require('axios')

const app = express()
app.use(express.json())

const BIN_ID = process.env.JSONBIN_BIN_ID
const API_KEY = process.env.JSONBIN_API_KEY
const JSONBIN_URL = `https://api.jsonbin.io/v3/b/${BIN_ID}`
const headers = { 'X-Master-Key': API_KEY, 'Content-Type': 'application/json' }

// Données en mémoire
let raids = []

// CORS pour Android
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*')
  res.header('Access-Control-Allow-Headers', '*')
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS')
  if (req.method === 'OPTIONS') return res.sendStatus(200)
  next()
})

// Charger depuis JSONBin au démarrage
async function loadData() {
  try {
    const res = await axios.get(JSONBIN_URL + '/latest', { headers })
    raids = res.data.record.raids || []
    console.log(`${raids.length} raids chargés depuis JSONBin ✅`)
  } catch (e) {
    console.error('Erreur chargement JSONBin:', e.message)
    raids = []
  }
}

// Sauvegarder sur JSONBin
async function saveData() {
  try {
    await axios.put(JSONBIN_URL, { raids }, { headers })
    console.log('Sauvegardé sur JSONBin ✅')
  } catch (e) {
    console.error('Erreur sauvegarde JSONBin:', e.message)
  }
}

// Prochain RAI_ID auto-incrémenté
function nextId() {
  if (raids.length === 0) return 1
  return Math.max(...raids.map(r => r.RAI_ID || 0)) + 1
}

// ─── ROUTES ───────────────────────────────────────────────────────────────────

// GET tous les raids
app.get('/raids', (req, res) => {
  res.json(raids)
})

// GET un raid par RAI_ID
app.get('/raids/:id', (req, res) => {
  const raid = raids.find(r => r.RAI_ID === parseInt(req.params.id))
  if (!raid) return res.status(404).json({ error: 'Not found' })
  res.json(raid)
})

// POST créer un raid
app.post('/raids', async (req, res) => {
  const raid = { ...req.body, RAI_ID: nextId() }
  raids.push(raid)
  await saveData()
  res.status(201).json(raid)
})

// PUT modifier un raid
app.put('/raids/:id', async (req, res) => {
  const idx = raids.findIndex(r => r.RAI_ID === parseInt(req.params.id))
  if (idx === -1) return res.status(404).json({ error: 'Not found' })
  raids[idx] = { ...req.body, RAI_ID: parseInt(req.params.id) }
  await saveData()
  res.json(raids[idx])
})

// PATCH modifier partiellement un raid
app.patch('/raids/:id', async (req, res) => {
  const idx = raids.findIndex(r => r.RAI_ID === parseInt(req.params.id))
  if (idx === -1) return res.status(404).json({ error: 'Not found' })
  raids[idx] = { ...raids[idx], ...req.body, RAI_ID: parseInt(req.params.id) }
  await saveData()
  res.json(raids[idx])
})

// DELETE supprimer un raid
app.delete('/raids/:id', async (req, res) => {
  const idx = raids.findIndex(r => r.RAI_ID === parseInt(req.params.id))
  if (idx === -1) return res.status(404).json({ error: 'Not found' })
  raids.splice(idx, 1)
  await saveData()
  res.json({})
})

// ─── PING (keep-alive) ────────────────────────────────────────────────────────
app.get('/ping', (req, res) => {
  res.json({ status: 'ok', uptime: process.uptime() })
})

// ─── DÉMARRAGE ────────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 3000
loadData().then(() => {
  app.listen(PORT, () => console.log(`API démarrée sur le port ${PORT} 🚀`))
})
