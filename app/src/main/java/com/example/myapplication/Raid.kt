package com.example.myapplication

import org.json.JSONObject

data class Raid(
    val id: Int = 0,
    val clubId: Int = 0,
    val organisateurId: Int = 0,
    val nom: String = "",
    val inscriptionDateDebut: String = "",
    val inscriptionDateFin: String = "",
    val dateDebut: String = "",
    val dateFin: String = "",
    val mail: String = "",
    val telephone: Long = 0,
    val lieu: String = "",
    val illustration: String? = null,
    val siteWeb: String? = null,
    val dateDemande: String = "",
    val status: String = "",
    val dateDecision: String? = null,
    val isDirty: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("CLU_ID", clubId)
        put("COM_ID_ORGANISATEUR_RAID", organisateurId)
        put("RAI_NOM", nom)
        put("RAI_INSCRIPTION_DATE_DEBUT", inscriptionDateDebut)
        put("RAI_INSCRIPTION_DATE_FIN", inscriptionDateFin)
        put("RAI_DATE_DEBUT", dateDebut)
        put("RAI_DATE_FIN", dateFin)
        put("RAI_MAIL", mail)
        put("RAI_TELEPHONE", telephone)
        put("RAI_LIEU", lieu)
        put("RAI_SITE_WEB", siteWeb)
        put("RAI_STATUS", status)
        put("RAI_DATE_DEMANDE", dateDemande)
    }

    companion object {
        fun fromJson(json: JSONObject): Raid = Raid(
            id                   = json.optInt("RAI_ID", 0),
            clubId               = json.optInt("CLU_ID", 0),
            organisateurId       = json.optInt("COM_ID_ORGANISATEUR_RAID", 0),
            nom                  = json.optString("RAI_NOM", ""),
            inscriptionDateDebut = json.optString("RAI_INSCRIPTION_DATE_DEBUT", ""),
            inscriptionDateFin   = json.optString("RAI_INSCRIPTION_DATE_FIN", ""),
            dateDebut            = json.optString("RAI_DATE_DEBUT", ""),
            dateFin              = json.optString("RAI_DATE_FIN", ""),
            mail                 = json.optString("RAI_MAIL", ""),
            telephone            = json.optLong("RAI_TELEPHONE", 0),
            lieu                 = json.optString("RAI_LIEU", ""),
            illustration         = json.optString("RAI_ILLUSTRATION")
                .takeIf { it != "null" && it.isNotEmpty() },
            siteWeb              = json.optString("RAI_SITE_WEB")
                .takeIf { it != "null" && it.isNotEmpty() },
            dateDemande          = json.optString("RAI_DATE_DEMANDE", ""),
            status               = json.optString("RAI_STATUS", ""),
            dateDecision         = json.optString("RAI_DATE_DECISION")
                .takeIf { it != "null" && it.isNotEmpty() }
        )
    }
}