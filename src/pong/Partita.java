// Pong — Programmazione Orientata agli Oggetti e Laboratori, Udine, a.a. 2026-27
package pong;

import giocatori.GiocatoreAutomatico;
import java.util.Random;

import geometria.Rettangolo;
import ostacoli.Livello;
import ostacoli.MotoreDeiLivelli;
import ostacoli.Ostacoli;
import ostacoli.Ostacolo;
import stelle.Raccolta;
import stelle.Stella;

/**
 * Una partita a un livello: voi a sinistra, il giocatore automatico a destra, due racchette,
 * una palla, gli ostacoli del livello e i conti di chi vince. Il livello si sceglie prima di cominciare.
 */
class Partita {
    /** Si gioca a 5, con almeno due punti di scarto (5 a 4 non basta, 6 a 4 sì). */
    static final int PUNTI_PER_VINCERE = 5;
    static final int SCARTO_MINIMO = 2;

    private static final int VITE_INIZIALI = 3;
    private static final int INTERVALLO_STELLA = 180;

    private final Campo campo;
    private final Giocatore giocatoreSinistra;
    private Giocatore giocatoreDestra;
    private final Racchetta racchettaSinistra;
    private final Racchetta racchettaDestra;
    private final Palla palla;
    private final Punteggio punteggio = new Punteggio(PUNTI_PER_VINCERE, SCARTO_MINIMO);
    private Livello livello;
    private Ostacoli ostacoli;
    private boolean iniziata;

    private final Livelli livelli = new Livelli(MotoreDeiLivelli.NUMERO_LIVELLI, VITE_INIZIALI, Lato.SINISTRA);;
    private final Raccolta raccolta = new Raccolta(Raccolta.STELLE_PER_UNA_VITA);
    private Stella stella;
    private int fotogrammiSenzaStella;

    /** Una partita fra umano, a sinistra, e il giocatore automatico, a destra; si comincia dal livello 1. */
    Partita(Campo campo, Giocatore umano) {
        this.campo = campo;
        this.giocatoreSinistra = umano;
        double centroY = campo.getAltezza() / 2.0 - Racchetta.ALTEZZA / 2.0;
        this.racchettaSinistra = new Racchetta(30, centroY);
        this.racchettaDestra = new Racchetta(campo.getLarghezza() - 30 - Racchetta.LARGHEZZA, centroY);
        this.palla = new Palla(campo.getLarghezza() / 2.0, campo.getAltezza() / 2.0);
        preparaLivello(livelli.getLivello());
    }

    /**
     * Prepara il livello numero: chiede al motore dei livelli ostacoli nuovi, velocità della palla ed
     * errore del giocatore automatico; rimette i punti a zero e la palla ferma al centro.
     * Pretende: la partita non è in corso; numero è fra 1 e MotoreDeiLivelli.NUMERO_LIVELLI.
     */
    void preparaLivello(int numero) {
        livello = MotoreDeiLivelli.livello(numero, campo.getLarghezza(), campo.getAltezza());
        ostacoli = livello.ostacoli();
        giocatoreDestra = new GiocatoreAutomatico(livello.erroreMassimo());
        punteggio.azzera();
        iniziata = false;
        stella = null;
        fotogrammiSenzaStella = 0;
        palla.fermaIn(campo.getLarghezza() / 2.0, campo.getAltezza() / 2.0);
    }

    // Quello che lo Schermo può chiedere: solo leggere. Il Punteggio resta qui dentro.

    /** I punti del lato chi. */
    int getPunti(Lato chi) { return punteggio.getPunti(chi); }

    /** Chi ha vinto; null se la partita non è finita. */
    Lato getVincitore() { return punteggio.getVincitore(); }

    /** Vero se si sta giocando: la partita è cominciata e non è finita. */
    boolean isInCorso() { return iniziata && !punteggio.isFinita(); }

    /** Vero se la partita è cominciata (anche se poi è finita). */
    boolean isIniziata() { return iniziata; }

    Livello getLivello() { return livello; }
    Ostacoli getOstacoli() { return ostacoli; }

    Livelli getLivelli() { return livelli; }
    Raccolta getRaccolta() { return raccolta; }
    Stella getStella() { return stella; }

    /**
     * Fa cominciare la partita e lancia la palla. Se una partita era già stata giocata, prima
     * prepara di nuovo lo stesso livello (ostacoli nuovi, punti a zero).
     * Pretende: la partita non è in corso.
     */
    void avvia() {
        if (livelli.isFinito()) {
            livelli.ricomincia();
            preparaLivello(livelli.getLivello());
        } else if (iniziata || punteggio.isFinita()) {
            preparaLivello(livelli.getLivello());
        }
        iniziata = true;
        lanciaPalla(new Random().nextBoolean());
    }

    /** Lancia la palla dal centro, alla velocità del livello. */
    private void lanciaPalla(boolean versoDestra) {
        palla.lancia(campo.getLarghezza() / 2.0, campo.getAltezza() / 2.0, versoDestra, livello.velocitaPalla());
    }

    /** Un passo del gioco: viene chiamato 60 volte al secondo. */
    void aggiorna() {
        if (!isInCorso()) return;
        ostacoli.avanzaTutti();
        muoviRacchette();
        palla.muovi();
        controllaRimbalzi();
        controllaOstacoli();
        aggiornaStella();
        controllaPunto();
    }

    private void aggiornaStella() {
        if (stella != null && stella.isVisibile()) {
            stella.avanza();
            if (stella.isVisibile() && racchettaToccaStella()) {
                stella.raccogli();
                if (raccolta.aggiungi()) {
                    livelli.guadagnaVita();
                }
            }
            return;
        }

        fotogrammiSenzaStella++;
        if (fotogrammiSenzaStella >= INTERVALLO_STELLA) {
            fotogrammiSenzaStella = 0;
            stella = creaStellaASinistra();
        }
    }

    private Stella creaStellaASinistra() {
        double dimensione = Racchetta.LARGHEZZA; // oppure es. 16 o 20, restando dentro la corsia
        double x = racchettaSinistra.getX();
        double maxY = Math.max(0, campo.getAltezza() - dimensione);
        double y = new Random().nextDouble() * maxY;
        Rettangolo areaStella = new Rettangolo(x, y, dimensione, dimensione);
        return new Stella(areaStella, livello.durataStella());
    }

    private boolean racchettaToccaStella() {
        Rettangolo areaRacchetta = new Rettangolo(
                racchettaSinistra.getX(),
                racchettaSinistra.getY(),
                racchettaSinistra.getLarghezza(),
                racchettaSinistra.getAltezza()
        );
        return areaRacchetta.tocca(stella.getArea());
    }

    /** Chiede a ciascun giocatore cosa vuole fare, e lo fa. Non sa chi sono i giocatori. */
    private void muoviRacchette() {
        Mossa mossaSinistra = giocatoreSinistra.decidiMossa(palla, racchettaSinistra);
        Mossa mossaDestra = giocatoreDestra.decidiMossa(palla, racchettaDestra);
        racchettaSinistra.muovi(mossaSinistra, campo);
        racchettaDestra.muovi(mossaDestra, campo);
    }

    private void controllaRimbalzi() {
        double y = palla.getPosizione().y();
        boolean toccaAlto = y - palla.getRaggio() <= 0 && palla.getVelocitaY() < 0;
        boolean toccaBasso = y + palla.getRaggio() >= campo.getAltezza() && palla.getVelocitaY() > 0;
        if (toccaAlto || toccaBasso) palla.rimbalzaVerticale();

        if (palla.tocca(racchettaSinistra) && palla.getVelocitaX() < 0) palla.rimbalzaSu(racchettaSinistra);
        if (palla.tocca(racchettaDestra) && palla.getVelocitaX() > 0) palla.rimbalzaSu(racchettaDestra);
    }

    /**
     * Se la palla tocca un ostacolo non rotto, rimbalza e l'ostacolo viene colpito (un ostacolo per passo).
     * Qui non si sa di che tipo è l'ostacolo: che cosa fa il colpo lo decide l'ostacolo.
     */
    private void controllaOstacoli() {
        Ostacolo colpito = ostacoli.ostacoloCheTocca(palla.getArea());
        if (colpito != null) {
            palla.rimbalzaSu(colpito.getArea());
            colpito.colpisci(ostacoli);
        }
    }

    /** Se la palla è uscita da un lato, il punto va all'altro lato. */
    private void controllaPunto() {
        double x = palla.getPosizione().x();
        if (x < 0) {
            punteggio.segnaPunto(Lato.DESTRA);
            dopoIlPunto(true);
        } else if (x > campo.getLarghezza()) {
            punteggio.segnaPunto(Lato.SINISTRA);
            dopoIlPunto(false);
        }
    }

    /** Dopo un punto: se la partita non è finita si serve verso chi ha subito il punto, altrimenti la palla si ferma. */
    private void dopoIlPunto(boolean versoSinistra) {
        if (!punteggio.isFinita()) {
            lanciaPalla(!versoSinistra);
            return;
        }

        palla.fermaIn(campo.getLarghezza() / 2.0, campo.getAltezza() / 2.0);
        iniziata = false;
        livelli.partitaFinita(punteggio.getVincitore());
        if (!livelli.isFinito()) {
            preparaLivello(livelli.getLivello());
        }
    }

    Campo getCampo() { return campo; }
    Palla getPalla() { return palla; }
    Racchetta getRacchettaSinistra() { return racchettaSinistra; }
    Racchetta getRacchettaDestra() { return racchettaDestra; }
}
