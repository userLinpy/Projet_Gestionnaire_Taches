package com.ghibli.todolist;

import java.io.File;
import java.text.DecimalFormat;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.cell.PropertyValueFactory; // Permet de lier automatiquement les colonnes aux attributs de l'objet Tache
import java.util.List;
import java.util.ArrayList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application {
    // 1. La liste "Master" qui stocke toutes les tâches en mémoire
    private ObservableList<Tache> masterData = FXCollections.observableArrayList();

    // Composants passés en variables d'instance pour être accessibles par la méthode deselectionnerTout()
    private TextField titleInput = new TextField();
    private TextArea descInput = new TextArea();
    private DatePicker dateFinPicker = new DatePicker();
    private ComboBox<String> priorityInput = new ComboBox<>();
    private Slider AvancSlider = new Slider(0, 100, 0);

    private Button addBtn = new Button("Ajouter");
    private Button updateBtn = new Button("Save Modifs");
    private Button clearBtn = new Button("Effacer");
    private Button deleteBtn = new Button("Supprimer");
    private Button finishedBtn = new Button("Terminer");

    private CheckBox selectAllBox = new CheckBox("Tout Sélectionner");

    private TextField searchField = new TextField(); // barre de recherches

    // Création d'un filtre filteredData
    // On ne laisse passer que les objects de types Tache qui respectent une condition (ici on accepte tout)
    private FilteredList<Tache> filteredData = new FilteredList<>(masterData, p -> true);
    // On ne verra que les taches filtrées
    // On connecte la tableView à la liste FILTRÉE, et non plus à la liste masterData.
    // Ainsi, si on change le filtre plus tard, la liste sur la fenêtre se mettra à jour toute seule.
    private TableView<Tache> tableView = new TableView<>(filteredData);

    // Création de la SortedList par-dessus la FilteredList
    private SortedList<Tache> sortedData = new SortedList<>(filteredData);

    // Création de la boite de filtrage (Terminées, En Cours, Toutes)
    private ComboBox<String> filterState = new ComboBox<>();
    // Création de la boite de filtrage (Toutes, Urgent, Important, Secondaire)
    private ComboBox<String> filterImportance = new ComboBox<>();

    // Création d'un objet image
    private ImageView noiraudeBasse = new ImageView();
    private boolean blockImageOverride = false; // va nous permettre de savoir quand changer l'image de la noiraude

    @Override
    public void start(Stage stage) {

        // Le titre de la fenêtre 
        stage.setTitle("Gestionnaire de Tâches Ghibli");

        // On charge les données s'il y en a 
        try {
            masterData.clear(); // On s'assure qu'elle est vide avant de charger
            chargerDonnees(); 
        } catch (Exception e) {
            System.err.println("Erreur de chargement, on démarre à vide.");
        }

        // Champ pour le titre 
        // on utilise TextField car le titre ne tient que sur une seule ligne
        titleInput.setPromptText("Titre de la tâche (Obligatoire)");

        // Champ pour la description
        descInput.setPromptText("Description détaillée...");
        descInput.setPrefRowCount(3);

        // Date de Fin (Obligatoire)
        dateFinPicker.setPromptText("Date d'échéance (Obligatoire).");

        // On crée un menu déroulant pour la priorité
        priorityInput.getItems().addAll("Urgent", "Important", "Secondaire");
        priorityInput.setPromptText("Choisir la priorité");
        priorityInput.setValue("Secondaire"); // Valeur par défaut

        // Avancement
        Label labelAvancement = new Label("Avancement : 0%");
        // Création du Slider d'avancement
        AvancSlider.setShowTickLabels(true); // Affiche les chiffres (0, 10, 20...)
        AvancSlider.setMajorTickUnit(25);    // Marque tous les 25%

        // Mettre à jour le label quand on bouge le curseur
        AvancSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            labelAvancement.setText("Avancement : " + newVal.intValue() + "%");
        });


        // Affichage visuel global du tableau
        tableView.setRowFactory(param -> new TableRow<Tache>() {
            @Override
            protected void updateItem(Tache item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle(""); // On réinitialise le style
                } else {
                    // Distinguer les tâches terminées
                    if (item.getAvancement() == 100.0) {
                        // On change la couleur 
                        setStyle("-fx-fill: gray; -fx-font-style: italic; -fx-opacity: 0.4; "); 
                    } else {
                        setStyle("-fx-fill: black; -fx-font-weight: bold; -fx-opacity: 1; ");
                    }
                }
            }
        });
        
        // Création des colonnes de la tableView 
        TableColumn<Tache, String> colName = new TableColumn<>("Tâche"); 
        TableColumn<Tache, java.time.LocalDate> colDate = new TableColumn<>("Date Échéance"); 
        TableColumn<Tache, Long> colRemainingDays = new TableColumn<>("Jours Restants");
        TableColumn<Tache, String> colPrio = new TableColumn<>("Priorité"); 
        TableColumn<Tache, Boolean> colState = new TableColumn<>("État");
        TableColumn<Tache, Double> colAvanc = new TableColumn<>("Avancement"); 

        // Connexion avec les attributs de la classe Tache via les Getters
        colName.setCellValueFactory( new PropertyValueFactory<>("titre"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateFinTache"));
        colRemainingDays.setCellValueFactory(cellData -> {
            Tache tache = cellData.getValue();
            if (tache.getDateFinTache() != null) {
                // CORRECTION UNIQUE : Si la tâche est finie, sa valeur numérique pour le tri devient TOUT DE SUITE 0
                if (tache.getAvancement() == 100.0) {
                    return new javafx.beans.property.SimpleLongProperty(0).asObject();
                }

                long jours = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), tache.getDateFinTache());
                return new javafx.beans.property.SimpleLongProperty(jours).asObject();
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });


        colPrio.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colState.setCellValueFactory(new PropertyValueFactory<>("terminee"));
        colAvanc.setCellValueFactory(new PropertyValueFactory<>("avancement"));

        // On personnalise l'affichage du texte avec des couleurs
        colRemainingDays.setCellFactory(column -> new TableCell<Tache, Long>() {
            @Override
            protected void updateItem(Long jours, boolean empty) {
                super.updateItem(jours, empty);
                if (empty || jours == null ) {
                    setText(null);
                    setStyle(""); // On réinitialise dans ce cas le style CSS
                }else {
                    // On vérifie d'abord que la ligne (TableRow) existe bien
                    TableRow<Tache> row = getTableRow();
                    Tache tacheSelected = (row != null) ? row.getItem() : null;

                    if (tacheSelected != null && tacheSelected.getAvancement() == 100.0) {
                        setText("" + 0);
                        setStyle("");
                    } else if ( jours < 0) {
                        setText("" + jours);
                        setStyle("-fx-text-fill : #b90000;");
                    } else if ( (0 <= jours) && (jours <= 1) ) {
                        setText("" + jours);
                        setStyle("-fx-text-fill : #d38900;");
                    } else {
                        setText("" + jours);
                        setStyle("");
                    }
                }

            }
        });

        // On personnalise l'affichage du texte (true -> "Terminée", false -> "En Cours")
        colState.setCellFactory(column -> new TableCell<Tache, Boolean>() {
            @Override
            protected void updateItem(Boolean terminee, boolean empty) {
                super.updateItem(terminee, empty);
                if (empty || terminee == null) { 
                    setText(null);
                }
                else { 
                    setText( terminee ? "Terminée" : "En Cours"); 
                }
            }
        });

        // On personnalise l'affichage de l'avancement
        colAvanc.setCellFactory(column -> new TableCell<Tache, Double>() {
            private final DecimalFormat df = new DecimalFormat("#,##");
            @Override
            protected void updateItem(Double avancement, boolean empty) {
                super.updateItem(avancement, empty);
                if (empty || avancement == null) {
                    setText(null);
                }
                else {
                    setText(df.format(avancement) + "%");
                }
            }
        });

        // Ajout des colonnes une par une pour éviter l'avertissement "Type safety" des varargs
        tableView.getColumns().add(colName);
        tableView.getColumns().add(colDate);
        tableView.getColumns().add(colRemainingDays);
        tableView.getColumns().add(colPrio);
        tableView.getColumns().add(colState);
        tableView.getColumns().add(colAvanc);

        // Redimension automatique pour occuper l'espace dans la colonne
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // On crée la ComboBox avec les trois options de filtrage
        filterState.getItems().addAll("Toutes", "En Cours", "Terminées");
        filterState.setValue("Toutes"); // Valeur par défaut au démarrage
        // On lui donne une largeur fixe ou on la laisse s'adapter
        filterState.setMaxWidth(200);
        
        // On crée la ComboBox avec les 4 options de filtrages
        filterImportance.getItems().addAll("Toutes", "Urgent", "Important", "Secondaire");
        filterImportance.setValue("Toutes");
        filterImportance.setMaxWidth(200);

        // On crée la barre de recherche
        searchField.setPromptText("🔍 Rechercher une tâche...");
        searchField.setPrefWidth(200);

        // Style du checkBox
        selectAllBox.setStyle(
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-background-color: #34495e; " +
            "-fx-background-radius: 5px; " +
            "-fx-padding: 4px 8px;"
        );

        // Boutons Désactivé par défaut
        updateBtn.setDisable(true); 
        deleteBtn.setDisable(true);
        finishedBtn.setDisable(true);

        // Organisation des boutons
        HBox buttonBox = new HBox(10, addBtn, updateBtn, clearBtn, deleteBtn, finishedBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        // On va charger les images de noiraudes ghibli
        // charge une image dans l'objet en appelant la méthode creerNoiraude définie en fin de fichier App.java
        noiraudeBasse = creerNoiraude("noiraudes_nothing.png");

        // On créé un objet qui prend l'espace qui reste sur la fenetre
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS); // Ce composant va manger tout l'espace vide disponible

        // On va séparer la fenêtre en deux cotés
        // Colonne gauche (formulaire)
        VBox gSide = new VBox(10, new Label("Nouvelle Tâche"), titleInput, descInput, dateFinPicker, new Label("Priorité : "), priorityInput, labelAvancement, AvancSlider, buttonBox, spacer, noiraudeBasse);
        gSide.setPadding(new Insets(15)); // Marge intérieure de 15 pixels de tous les côtés

        // Colonne droite (tableView)
        HBox filterZone = new HBox(20, filterState, filterImportance, searchField, selectAllBox);
        VBox dSide = new VBox(10, new Label("Vos Tâches"), filterZone, tableView);
        dSide.setPadding(new Insets(15));

        // Configuration de la taille des composants
        gSide.setPrefWidth(350);
        gSide.setMinWidth(350);
        
        // On autorise les deux côtés à grandir proportionnellement
        HBox.setHgrow(gSide, Priority.ALWAYS);
        HBox.setHgrow(dSide, Priority.ALWAYS);
        // On autorise les boutons à grandir proportionnellement
        HBox.setHgrow(addBtn, Priority.ALWAYS);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        HBox.setHgrow(updateBtn, Priority.ALWAYS);

        // On lie la hauteur de la tableView à son parent pour éviter les bugs de Double.MAX_VALUE
        tableView.prefHeightProperty().bind(dSide.heightProperty());
        
        // On donne une largeur maximale raisonnable aux champs
        titleInput.setMaxWidth(1000);
        descInput.setMaxWidth(1000);
        dateFinPicker.setMaxWidth(1000);
        priorityInput.setMaxWidth(1000);
        // On donne la taille maximale aux boutons
        addBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.setMaxWidth(Double.MAX_VALUE);

        // Fonctionnalités

        // On crée la bulle de texte avec les astuces
        // Cette bulle explique les touches aux utilisateurs
        Tooltip bullePensee = new Tooltip(
            "💡 Les astuces de la Noiraude :\n\n" +
            "• Multi-sélection : Maintenez CTRL + Clic pour choisir plusieurs tâches.\n" +
            "• Tri cumulé : Maintenez MAJ (Shift) + Clic sur les colonnes pour combiner les tris !"
        );

        // On ajuste juste les timings
        bullePensee.setShowDelay(javafx.util.Duration.millis(100)); 
        bullePensee.setShowDuration(javafx.util.Duration.seconds(10)); 

        // On installe la bulle sur l'image de la Noiraude Basse
        Tooltip.install(noiraudeBasse, bullePensee);

        // On appelle notre méthode qui active le filtre
        filterState.valueProperty().addListener((obs, oldVal, newVal) -> {
            filterRefresh(); 
            tableView.getSelectionModel().clearSelection(); // On désélectionne les tâches);
        });

        filterImportance.valueProperty().addListener((obs, oldVal, newVal) -> {
            filterRefresh();
            tableView.getSelectionModel().clearSelection(); // On désélectionne les tâches
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterRefresh();
            tableView.getSelectionModel().clearSelection();
        });

        // On programme notre checkBox qui sélectionne toutes les tâches
        selectAllBox.setOnAction(e -> {
            // Si la case est cochée, on sélectionne toutes les tâches visibles sur tableView
            if (selectAllBox != null && selectAllBox.isSelected()) {
                for (Tache t : tableView.getItems()) {
                    tableView.getSelectionModel().select(t);
                }
                selectAllBox.setSelected(true);
            } else {
            // Si on décoche la checkBox, on déselectionne tous les tâches du tableau tableView
                selectAllBox.setSelected(false);
                tableView.getSelectionModel().clearSelection();
            }
        });

        // Tri chronologique (du plus proche au plus loin )
        sortedData.setComparator((tache1, tache2) -> tache1.getDateFinTache().compareTo(tache2.getDateFinTache()));
        // On lie le comparateur de la SortedList à celui de la TableView
        // Cela permet à JavaFX de mettre à jour la liste dès que l'utilisateur clique sur une colonne
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        // On injecte la SortedList finale dans le tableau
        tableView.setItems(sortedData);

        // Active la sélection multiple sur le tableau
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        addBtn.setOnAction(e -> {
            if (titleInput.getText().trim().isEmpty() || dateFinPicker.getValue() == null) {
                new Alert(Alert.AlertType.ERROR, "Le titre et la date sont obligatoires !").show();
            } else {
                Tache t = new Tache(titleInput.getText(), descInput.getText(), dateFinPicker.getValue(), priorityInput.getValue());
                t.setAvancement(AvancSlider.getValue());
                masterData.add(t);
                sauvegarderDonnees();
                viderTout();
                changerImageNoiraude("noiraudes_rocher.png");
            }
        });

        clearBtn.setOnAction(e -> {viderTout(); changerImageNoiraude("noiraudes_nothing.png");}); // Ici il suffit d'appeler la méthode qu'on a créé 

        deleteBtn.setOnAction(e -> {
            // On récupère la liste de toutes les tâches actuellement sélectionnées
            ObservableList<Tache> selecTasks = tableView.getSelectionModel().getSelectedItems();
            if (!selecTasks.isEmpty()) {

                // Si on ne sélectionne qu'une seule tâche
                if (selecTasks.size() == 1) {
                    Alert alert1 = new Alert(AlertType.CONFIRMATION);
                    alert1.setTitle("Suppression Singulière");
                    alert1.setHeaderText("Supprimer la tâche sélectionnée");
                    alert1.setContentText("Voulez-vous vraiment supprimer la tâche sélectionnée ?");
                    alert1.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        masterData.removeAll(selecTasks);
                        sauvegarderDonnees();
                        viderTout();
                        changerImageNoiraude("noiraudes_sat.png");
                    }
                    tableView.getSelectionModel().clearSelection(); // On désélectionne les tâches
                    });
                }
                // Si on sélectionne plus d'une tâche à la fois
                else if (selecTasks.size() > 1 ) {
                    // Boîte de dialogue pour confirmer la suppression de groupe
                    Alert alert2 = new Alert(AlertType.CONFIRMATION);
                    alert2.setTitle("Supression Groupée");
                    alert2.setHeaderText("Supprimer les tâches sélectionnées");
                    alert2.setContentText("Voulez-vous vraiment supprimer les " + selecTasks.size() + " tâches sélectionnées ?");
                    
                    alert2.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            // Pour éviter les conflits d'index pendant la suppression,
                            // on fait une copie temporaire de la sélection
                            List<Tache> delTasks = new ArrayList<>(selecTasks);
                            masterData.removeAll(delTasks);
                            sauvegarderDonnees();
                            viderTout();
                            tableView.getSelectionModel().clearSelection(); // On désélectionne les tâches
                            filterRefresh();
                            changerImageNoiraude("noiraudes_sat.png");
                        } 
                        tableView.getSelectionModel().clearSelection(); // On désélectionne les tâches
                    });
                }
            }
        });

        updateBtn.setOnAction(e -> {
            Tache t = tableView.getSelectionModel().getSelectedItem();
            if (t != null) {
                t.setTitre(titleInput.getText());
                t.setDescription(descInput.getText());
                t.setDateFinTache(dateFinPicker.getValue());
                t.setPriorite(priorityInput.getValue());
                t.setAvancement(AvancSlider.getValue());
                sauvegarderDonnees();
                viderTout();
                tableView.getSelectionModel().clearSelection(); // On désélectionne la tâche 
                changerImageNoiraude("noiraudes_rocher.png");
                filterRefresh();
            }
        });

        finishedBtn.setOnAction(e -> {
            ObservableList<Tache> tachesSelectionnees = tableView.getSelectionModel().getSelectedItems();

            if (!tachesSelectionnees.isEmpty()) {
                List<Tache> tachesAModifier = new ArrayList<>(tachesSelectionnees);

                boolean toutesTerminees = true;
                for (Tache t : tachesAModifier) {
                    if (t != null && t.getAvancement() < 100.0) {
                        toutesTerminees = false;
                        break;
                    }
                }

                double nouvelAvancement;
                if (toutesTerminees) {
                    nouvelAvancement = 0.0;
                    changerImageNoiraude("stone_running.png");
                } else {
                    nouvelAvancement = 100.0;
                    changerImageNoiraude("noiraudes_stars.png");
                }
            
                for (Tache t : tachesAModifier) {
                    if (t != null) {
                        t.setAvancement(nouvelAvancement);
                    }
                }

                // 🌟 ACTIVATION DU VERROU : On interdit à l'écouteur de sélection de modifier l'image
                blockImageOverride = true;

                viderTout();
                tableView.getSelectionModel().clearSelection(); 
                tableView.refresh();                           
                filterRefresh();                               
                sauvegarderDonnees();                          

                for (Tache t : tachesAModifier) {
                    if (tableView.getItems().contains(t)) {
                        tableView.getSelectionModel().select(t);
                    }
                }

                // 🌟 DÉSACTIVATION DU VERROU : L'application reprend son comportement normal
                blockImageOverride = false;

            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attention");
                alert.setHeaderText(null);
                alert.setContentText("Veuillez sélectionner une ou plusieurs tâches à marquer comme terminées.");
                alert.showAndWait();
            }
        });
        
        // On remplit le formulaire et gère les boutons selon le NOMBRE de tâches sélectionnées
        tableView.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<Tache>) change -> {
            // On récupère la liste de toutes les tâches sélectionnées
            var selectedItems = tableView.getSelectionModel().getSelectedItems();
            int nbSelection = selectedItems.size();
        
            if (nbSelection == 1) {
                // CAS 1 : UNE SEULE tâche est sélectionnée -> Mode modification classique
                Tache uniqueTache = selectedItems.get(0);

                titleInput.setText(uniqueTache.getTitre());
                descInput.setText(uniqueTache.getDescription());
                dateFinPicker.setValue(uniqueTache.getDateFinTache());
                priorityInput.setValue(uniqueTache.getPriorite());
                AvancSlider.setValue(uniqueTache.getAvancement());

                selectAllBox.setSelected(false);
                updateBtn.setDisable(false); // On peut modifier
                addBtn.setDisable(true);
                deleteBtn.setDisable(false); // On peut supprimer
                clearBtn.setDisable(false);
                finishedBtn.setDisable(false);
                if (!blockImageOverride) { changerImageNoiraude("stone_running.png"); }

            } else if (nbSelection > 1) {
                // CAS 2 : PLUSIEURS tâches sont sélectionnées -> Mode action groupée

                updateBtn.setDisable(true);  // Désactivé (impossible de modifier plusieurs tâches à la fois)
                addBtn.setDisable(true);
                deleteBtn.setDisable(false); // Activé : On a le droit de SUPPRIMER le groupe sélectionné !
                clearBtn.setDisable(true);  // Activé : Permet de tout désélectionner d'un coup
                finishedBtn.setDisable(false); // Activé pour terminer plusieurs tâches à la fois
            } else {
                // CAS 3 : AUCUNE tâche n'est sélectionnée -> Mode création initial
                viderTout();
                updateBtn.setDisable(true);
                addBtn.setDisable(false);
                deleteBtn.setDisable(true);
                clearBtn.setDisable(false);
                finishedBtn.setDisable(true);
                if (!blockImageOverride) {
                    changerImageNoiraude("noiraudes_nothing.png"); 
                }
            }
        });

        // On concatène les deux cotés horizonatelemnt
        HBox mainLayout = new HBox(gSide, dSide);

        // On donne un nom de classe au conteneur de l'application
        mainLayout.getStyleClass().add("fond-application");

        // Gestion du clic pour tout désélectionner, rénitialiser les boutons, et les champs
        gSide.setOnMouseClicked(event -> {
            if (event.getTarget() == gSide) { deselectionnerTout();}
        });

        mainLayout.setOnMouseClicked(event -> {
            if (event.getTarget() == mainLayout) { deselectionnerTout(); }
        });

        spacer.setOnMouseClicked(event -> {
            if (event.getTarget() == spacer) { 
                deselectionnerTout(); 
                tableView.requestFocus(); // Donne le focus au tableau, ce qui libère la barre de recherche
            }
        });

        // Affichage des composants dans la fenêtre
        Scene scene = new Scene(mainLayout, 1000, 600);
        // On récupère le fichier CSS 
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }

    // Méthode pour vider les champs et rénitialiser les boutons
    private void viderTout() {
        titleInput.clear();
        descInput.clear();
        dateFinPicker.setValue(null);
        priorityInput.setValue("Secondaire");
        AvancSlider.setValue(0);
        deleteBtn.setDisable(true);
        updateBtn.setDisable(true);
        addBtn.setDisable(false);
        finishedBtn.setDisable(true);
        tableView.refresh();
    }

    // permet de quitter une tâche si on clique autrepart
    private void deselectionnerTout() {
        // On vérifie s'il y a une sélection en cours
        Tache selectionnee = tableView.getSelectionModel().getSelectedItem();
        // On désélectionne la liste dans tous les cas
        tableView.getSelectionModel().clearSelection();
        selectAllBox.setSelected(false); // On déselectionne la checkBox au cas où
        // SECURITÉ : On ne vide les champs QUE si on était en train de modifier une tâche
        // Si selectionnee est null, cela veut dire que l'utilisateur rédige une NOUVELLE tâche.
        // On part du principe que perdre ce qu'on a écrit lors d'une modif d'une par un missclick est moins grave que si c'était la création d'une nouvelle tâche
        if (selectionnee != null) { viderTout(); }
        changerImageNoiraude("noiraudes_nothing.png");
    }

    private void filterRefresh() {
        // Dès qu'on modifie un filtre ou la case "Tout sélectionner", on décoche cette case.
        if (selectAllBox != null && selectAllBox.isSelected()) {
            selectAllBox.setSelected(false);
        }

        filteredData.setPredicate(tache -> {
            // FILTRE TEXTUEL (Colonne Tâche) 
            String textSearch = (searchField != null) ? searchField.getText().trim().toLowerCase() : "";
            if (!textSearch.isEmpty()) {
                // Si le titre ET la description ne contiennent pas le mot recherché, on rejette la tâche
                boolean matchTitre = (tache.getTitre() != null && tache.getTitre().toLowerCase().contains(textSearch) );
                boolean matchDesc = ( tache.getDescription() != null && tache.getDescription().toLowerCase().contains(textSearch) );
                if (!matchTitre && !matchDesc) {
                    return false; 
                }
            }

            // FILTRE D'ÉTAT (ComboBox Radio) 
            String stateVal = filterState.getValue();
            if (stateVal != null) {
                if (stateVal.equals("En Cours") && tache.getAvancement() == 100.0) {
                    return false; // On rejette les tâches terminées si on veut uniquement celles "En Cours"
                }
                if (stateVal.equals("Terminées") && tache.getAvancement() < 100.0) {
                    return false; // On rejette les tâches en cours si on veut uniquement les "Terminées"
                }
            }

            // FILTRE D'IMPORTANCE (ComboBox Radio) 
            String prioVal = filterImportance.getValue();
            if (prioVal != null && !prioVal.equals("Toutes")) {
                if (!tache.getPriorite().equals(prioVal)) {
                    return false; // On rejette si la priorité ne correspond pas exactement
                }
            }

            // Si la tâche a survécu à tous les filtres, on l'affiche !
            return true;
        });
    }

    // On définit un chemin universel vers le dossier utilisateur
    // La raison pour laquelle on crée cette méthode est pour éviter que Windows bloque l'accès à la création du fichier csv ou à son accès
    private String getCheminSauvegarde() {
        // Récupère le dossier AppData/Roaming (standard pour les sauvegardes)
        String chemin = System.getenv("APPDATA");
        
        // Si AppData n'existe pas, on se replie sur le dossier utilisateur
        if (chemin == null) {
            chemin = System.getProperty("user.home");
        }

        // On crée un sous-dossier pour notre application
        File dossier = new File(chemin + File.separator + "GhibliTodo");
        if (!dossier.exists()) {
            dossier.mkdirs(); // Crée le dossier s'il n'existe pas
        }

        return dossier.getAbsolutePath() + File.separator + "taches.csv";
    }

    // On va créer une fonction pour sauvegarder les données dans un fichier csv
    // Même méthodologie que sur pandas en python, donc assez simple
    private void sauvegarderDonnees() {
        // On utilise le chemin universel
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.File(getCheminSauvegarde()))) {
            for (Tache t : masterData) {
                writer.println(t.getTitre() + ";" + 
                               t.getDescription().replace(";", ",") + ";" + 
                               t.getDateFinTache() + ";" + 
                               t.getPriorite() + ";" + 
                               t.getAvancement());
            }
            System.out.println("Sauvegardé dans : " + getCheminSauvegarde());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
    
    // On va créer une fonction/méthode pour charger les données depuis un fichier csv
    // Même méthodologie que sur python avec pandas
    private void chargerDonnees() {
        java.io.File fichier = new java.io.File(getCheminSauvegarde());
        if (!fichier.exists()) return;
        
        try (java.util.Scanner scanner = new java.util.Scanner(fichier)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) continue;
                try {
                    String[] data = line.split(";");
                    if (data.length == 5) {
                        Tache t = new Tache(data[0], data[1], java.time.LocalDate.parse(data[2]), data[3]);
                        t.setAvancement(Double.parseDouble(data[4]));
                        masterData.add(t);
                    }
                } catch (Exception e) {
                    // Si UNE ligne est corrompue, on l'ignore et on passe à la suivante
                    System.err.println("Ligne ignorée (format invalide) : " + line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // on va créer une méthode qui charge une image spécifique de noiraudes ghibli
    private ImageView creerNoiraude(String nomFichier) {
        // On charge l'image depuis les ressources
        Image img = new Image(getClass().getResourceAsStream("/" + nomFichier));
        ImageView iv = new ImageView(img);
        
        // On définit une largeur fixe 
        iv.setFitWidth(120);
        iv.setPreserveRatio(true);
        
        // Lisser l'image pour éviter les pixels si elle est agrandie
        iv.setSmooth(true);
        return iv;
    }
    // On va créer une méthode pour changer l'image du noiraude en cours de route
    private void changerImageNoiraude(String nomFichier) {
        Image nouvelleImg = new Image(getClass().getResourceAsStream("/" + nomFichier));
        noiraudeBasse.setImage(nouvelleImg);
    }

    public static void main(String[] args) { launch(); }
}