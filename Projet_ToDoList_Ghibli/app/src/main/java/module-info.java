module com.ghibli.todolist {
    // On ajoute 'transitive' pour que les types de JavaFX (comme Stage) 
    // soient accessibles partout dans l'application.
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires javafx.base;
    
    // Nécessaire pour que JavaFX puisse manipuler les objets Tache 
    opens com.ghibli.todolist to javafx.fxml, javafx.base, javafx.graphics;

    exports com.ghibli.todolist;
}