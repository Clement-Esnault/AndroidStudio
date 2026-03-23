import com.example.myapplication.Raid
import com.example.myapplication.RaidApiService
import kotlin.collections.toMutableList

object RaidManager {
    // La liste "Client" qui sera affichée dans ton interface
    var listRaids = mutableListOf<Raid>()

    // Charger depuis l'API
    fun loadFromServer(token: String, onComplete: (Boolean) -> Unit) {
        Thread {
            RaidApiService.getAllRaids(token).onSuccess { raids ->
                listRaids = raids.toMutableList()
                onComplete(true)
            }.onFailure { onComplete(false) }
        }.start()
    }

    // Modifier CÔTÉ CLIENT + Envoi SERVEUR
    fun updateRaid(updatedRaid: Raid, token: String) {
        // 1. Modif immédiate côté client (mémoire)
        val index = listRaids.indexOfFirst { it.id == updatedRaid.id }
        if (index != -1) {
            listRaids[index] = updatedRaid
        }

        // 2. Envoi asynchrone au serveur pour le sujet
        Thread {
            RaidApiService.updateRaid(updatedRaid, token).onFailure {
                // Optionnel : gérer ici si le serveur refuse (ex: remettre l'ancienne valeur)
                println("Note: Echec synchro serveur (401?), mais gardé en local.")
            }
        }.start()
    }
}