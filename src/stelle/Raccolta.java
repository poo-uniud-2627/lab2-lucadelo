// Pong — Programmazione Orientata agli Oggetti e Laboratori, Udine, a.a. 2026-27
// Consegna 5, variante 2: le stelle. I contratti ci sono, i campi e i corpi dei metodi li scrivete voi.
package stelle;

/** Il conto delle stelle raccolte: ogni stellePerUnaVita stelle si riguadagna una vita. */
public class Raccolta {
    /** Nel gioco: cinque stelle per una vita. */
    public static final int STELLE_PER_UNA_VITA = 5;
    private final int stellePerUnaVita;
    private int stelleRaccolte;

    /**
     * Un conto nuovo, a zero.
     * Lancia IllegalArgumentException se stellePerUnaVita è minore di 1.
     */
    public Raccolta(int stellePerUnaVita) {
        if(stellePerUnaVita < 1)
            throw new IllegalArgumentException("stellePerUnaVita è minore di 1");

        this.stellePerUnaVita = stellePerUnaVita;
        this.stelleRaccolte = 0;
    }

    /**
     * Una stella in più.
     * Promette: vero se con questa stella si arriva a stellePerUnaVita: allora il conto torna a 0.
     *           Altrimenti falso, e il conto cresce di 1.
     */
    public boolean aggiungi() {
        stelleRaccolte++;
        if(stelleRaccolte >= stellePerUnaVita){
            stelleRaccolte = 0;
            return true;
        }
        return false;
    }

    /** Le stelle raccolte dall'ultima vita guadagnata. Promette: fra 0 e stellePerUnaVita - 1. */
    public int getStelle() {
        return stelleRaccolte;
    }
}
