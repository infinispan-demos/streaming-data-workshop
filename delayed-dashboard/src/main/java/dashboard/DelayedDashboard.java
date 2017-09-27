package dashboard;

import javafx.application.Application;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Java FX application that stores station board information
 * and uses continuous query to keep a centralised dashboard
 * of all the delays happening.
 */
public class DelayedDashboard extends Application {

  private final TableView<DelayedTrainView> table = new TableView<>();
  private final ExecutorService exec = Executors.newSingleThreadExecutor();
  private DelayedDashboardTask task;

  @Override
  public void start(Stage stage) {
    BorderPane root = new BorderPane();
    Scene scene = new Scene(root, 800, 600);

    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setEditable(true);

    TableColumn typeCol = getTableCol("Type", 10, "type");
    TableColumn departureCol = getTableCol("Departure", 30, "departure");
    TableColumn stationCol = getTableCol("Station", 200, "station");
    TableColumn destinationCol = getTableCol("Destination", 200, "destination");
    TableColumn delayCol = getTableCol("Delay", 20, "delay");
    TableColumn trainName = getTableCol("Train Name", 50, "trainName");

    table.getColumns().addAll(
      typeCol, departureCol, stationCol, destinationCol, delayCol, trainName);

    root.setCenter(table);

    task = new DelayedDashboardTask();
    SortedList<DelayedTrainView> sorted = new SortedList<>(
      task.getPartialResults(), DelayedTrainView.comparator());
    table.setItems(sorted);
    task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
      if(newValue != null) {
        Exception ex = (Exception) newValue;
        ex.printStackTrace();
      }
    });

    exec.submit(task);

    stage.setOnCloseRequest(we -> {
      this.stop();
      System.out.println("Bye.");
    });

    stage.setTitle("Swiss Transport Delays Board");
    stage.setScene(scene);
    stage.show();
  }

  private TableColumn getTableCol(String colName, int minWidth, String fieldName) {
    TableColumn<DelayedTrainView, String> typeCol = new TableColumn<>(colName);
    typeCol.setMinWidth(minWidth);
    typeCol.setCellValueFactory(new PropertyValueFactory<>(fieldName));
    return typeCol;
  }

  @Override
  public void stop() {
    if (task != null)
      task.cancel();

    exec.shutdown();
  }

  public static void main(String[] args) {
    launch(args);
  }

}
