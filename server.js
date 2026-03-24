const express = require('express')
const { Pool } = require('pg')

const app = express()
app.use(express.json())

// CORS pour Android
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*')
  res.header('Access-Control-Allow-Headers', '*')
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS')
  if (req.method === 'OPTIONS') return res.sendStatus(200)
  next()
})

// Connexion PostgreSQL via variable d'environnement Render
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
})

// ─── INIT BDD ────────────────────────────────────────────────────────────────
async function initDB() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS raids (
      "CLU_ID" INTEGER,
      "RAI_ID" SERIAL PRIMARY KEY,
      "COM_ID_ORGANISATEUR_RAID" INTEGER,
      "RAI_NOM" TEXT,
      "RAI_INSCRIPTION_DATE_DEBUT" DATE,
      "RAI_INSCRIPTION_DATE_FIN" DATE,
      "RAI_DATE_DEBUT" DATE,
      "RAI_DATE_FIN" DATE,
      "RAI_MAIL" TEXT,
      "RAI_TELEPHONE" BIGINT,
      "RAI_LIEU" TEXT,
      "RAI_ILLUSTRATION" TEXT,
      "RAI_SITE_WEB" TEXT,
      "RAI_DATE_DEMANDE" DATE,
      "RAI_STATUS" TEXT,
      "RAI_DATE_DECISION" DATE
    );
  `)

  // Insérer les données initiales si la table est vide
  const { rows } = await pool.query('SELECT COUNT(*) FROM raids')
  if (parseInt(rows[0].count) === 0) {
    await pool.query(`
      INSERT INTO raids ("CLU_ID","COM_ID_ORGANISATEUR_RAID","RAI_NOM","RAI_INSCRIPTION_DATE_DEBUT","RAI_INSCRIPTION_DATE_FIN","RAI_DATE_DEBUT","RAI_DATE_FIN","RAI_MAIL","RAI_TELEPHONE","RAI_LIEU","RAI_DATE_DEMANDE","RAI_STATUS")
      VALUES
        (1,12,'Raid O''Bivwak','2026-01-09','2026-04-30','2026-05-23','2026-05-24','mail@mail',1234567890,'Parc des Noues','2026-01-09','VALIDE'),
        (1,22,'RAID CHAMPETRE','2025-08-10','2025-10-30','2025-11-01','2025-11-02','ad@ad',123456789,'PARC INTERCOMMUNAL DEBREUIL 77000 MELUN','2026-01-09','VALIDE')
    `)
  }

  console.log('Table raids initialisée ✅')
}

// ─── ROUTES RAIDS ─────────────────────────────────────────────────────────────
app.get('/raids', async (req, res) => {
  const { rows } = await pool.query('SELECT * FROM raids ORDER BY "RAI_ID"')
  res.json(rows)
})

app.get('/raids/:id', async (req, res) => {
  const { rows } = await pool.query('SELECT * FROM raids WHERE "RAI_ID" = $1', [req.params.id])
  if (!rows.length) return res.status(404).json({ error: 'Not found' })
  res.json(rows[0])
})

app.post('/raids', async (req, res) => {
  const b = req.body
  const { rows } = await pool.query(`
    INSERT INTO raids ("CLU_ID","COM_ID_ORGANISATEUR_RAID","RAI_NOM","RAI_INSCRIPTION_DATE_DEBUT","RAI_INSCRIPTION_DATE_FIN","RAI_DATE_DEBUT","RAI_DATE_FIN","RAI_MAIL","RAI_TELEPHONE","RAI_LIEU","RAI_ILLUSTRATION","RAI_SITE_WEB","RAI_DATE_DEMANDE","RAI_STATUS","RAI_DATE_DECISION")
    VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15) RETURNING *`,
    [b.CLU_ID,b.COM_ID_ORGANISATEUR_RAID,b.RAI_NOM,b.RAI_INSCRIPTION_DATE_DEBUT,b.RAI_INSCRIPTION_DATE_FIN,b.RAI_DATE_DEBUT,b.RAI_DATE_FIN,b.RAI_MAIL,b.RAI_TELEPHONE,b.RAI_LIEU,b.RAI_ILLUSTRATION,b.RAI_SITE_WEB,b.RAI_DATE_DEMANDE,b.RAI_STATUS,b.RAI_DATE_DECISION]
  )
  res.status(201).json(rows[0])
})

app.put('/raids/:id', async (req, res) => {
  const b = req.body
  const { rows } = await pool.query(`
    UPDATE raids SET
      "CLU_ID"=$1,"COM_ID_ORGANISATEUR_RAID"=$2,"RAI_NOM"=$3,
      "RAI_INSCRIPTION_DATE_DEBUT"=$4,"RAI_INSCRIPTION_DATE_FIN"=$5,
      "RAI_DATE_DEBUT"=$6,"RAI_DATE_FIN"=$7,"RAI_MAIL"=$8,
      "RAI_TELEPHONE"=$9,"RAI_LIEU"=$10,"RAI_ILLUSTRATION"=$11,
      "RAI_SITE_WEB"=$12,"RAI_DATE_DEMANDE"=$13,"RAI_STATUS"=$14,"RAI_DATE_DECISION"=$15
    WHERE "RAI_ID"=$16 RETURNING *`,
    [b.CLU_ID,b.COM_ID_ORGANISATEUR_RAID,b.RAI_NOM,b.RAI_INSCRIPTION_DATE_DEBUT,b.RAI_INSCRIPTION_DATE_FIN,b.RAI_DATE_DEBUT,b.RAI_DATE_FIN,b.RAI_MAIL,b.RAI_TELEPHONE,b.RAI_LIEU,b.RAI_ILLUSTRATION,b.RAI_SITE_WEB,b.RAI_DATE_DEMANDE,b.RAI_STATUS,b.RAI_DATE_DECISION,req.params.id]
  )
  if (!rows.length) return res.status(404).json({ error: 'Not found' })
  res.json(rows[0])
})

app.delete('/raids/:id', async (req, res) => {
  await pool.query('DELETE FROM raids WHERE "RAI_ID" = $1', [req.params.id])
  res.json({})
})

// ─── DÉMARRAGE ────────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 3000
initDB().then(() => {
  app.listen(PORT, () => console.log(`API Raids démarrée sur le port ${PORT} 🚀`))
}).catch(err => {
  console.error('Erreur init BDD:', err)
  process.exit(1)
})
