package com.ghibli.todolist;
import java.time.LocalDate;

public class Tache {
    private String titre; 
    private String description;
    private LocalDate dateCreation;
    private LocalDate dateFinTache;
    private String priorite;    // Urgent, Important, Secondaire
    private double avancement;
    private boolean terminee; // pour indiquer l'état de la tache

    // Constructeur Tache
    public Tache(String titre, String description, LocalDate dateFinTache, String priorite) {
        this.titre = titre;
        this.description = description;
        this.dateCreation = LocalDate.now();
        this.dateFinTache = dateFinTache;
        this.priorite = priorite;
        this.avancement = 0.0; // par defaut l'avancement est à 0%
        this.terminee = false;  // par défaut la tache n'est pas encore terminée
    }

    // Getters et Setters des différents attributs
    public String getTitre(){ return titre;}
    public void setTitre(String titre) { this.titre = titre;}

    public String getDescription(){ return description;}
    public void setDescription(String description) { this.description = description;}

    public LocalDate getDateCreation() { return dateCreation;}
    public void setDateCreation(LocalDate date) { this.dateCreation = date;}

    public LocalDate getDateFinTache() { return dateFinTache;}
    public void setDateFinTache(LocalDate date2) { this.dateFinTache = date2;}

    public String getPriorite() { return priorite;}
    public void setPriorite(String priorite_new) { this.priorite = priorite_new;}
    
    public double getAvancement() { return avancement;}
    public void setAvancement(double avanc) { this.avancement = avanc;} 

    public boolean getTerminee() { 
        if (avancement == 100.0) {
            terminee = true;
        }else {terminee = false;}
        return terminee;}

    public void setTerminee(boolean bool) { this.terminee = bool;}

//    @Override
//    public String toString() {
//        return String.format("%s - %s - %s - [%s] - %d%%"
//            ,titre, dateFinTache, priorite,  (terminee ? "FINIE" : "EN COURS"), (int) avancement);
//    }
}
