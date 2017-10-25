package workshop.shared;

// TODO: If this is the only shared thing, moved it to data-model
public final class Constants {

  public static final String STATION_BOARDS_CACHE_NAME = "station-boards";
  public static final String DELAYED_TRAINS_CACHE_NAME = "delayed-trains";

  public static final String STATION_BOARD_PROTO = "/station-board.proto";
  public static final String TRAIN_POSITION_PROTO = "/train-position.proto";

  public static final String DATAGRID_HOST = "datagrid-hotrod";
  public static final int DATAGRID_PORT = 11222;

  public static final String WORKSHOP_MAIN_HOST = "workshop-main";
  public static final String WORKSHOP_MAIN_URI = "/inject";
  public static final String STATIONS_INJECTOR_HOST = "stations-injector";
  public static final String STATIONS_INJECTOR_URI = "/inject";
  public static final String DELAYED_LISTENER_HOST = "delayed-listener";
  public static final String DELAYED_LISTENER_URI = "/listen";

}
