package workshop.shared;

public final class Constants {

  public static final String STATION_BOARDS_CACHE_NAME = "station-boards";
  public static final String TRAIN_POSITIONS_CACHE_NAME = "train-positions";
  public static final String TRAIN_POSITIONS_TOPIC = "train-positions";
  public static final String STATION_BOARDS_TOPIC = "station-boards";
  public static final String DELAYED_TRAINS_CACHE_NAME = "delayed-trains";

  public static final String STATION_BOARD_PROTO = "/station-board.proto";
  public static final String TRAIN_POSITION_PROTO = "/train-position.proto";

  public static final String DATAGRID_HOST = "datagrid-hotrod";
  public static final int DATAGRID_PORT = 11222;

  public static final String WORKSHOP_MAIN_HOST = "workshop-main";
  public static final String WORKSHOP_MAIN_URI = "/inject";
  public static final String STATIONS_INJECTOR_HOST = "stations-injector";
  public static final String STATIONS_INJECTOR_URI = "/inject";
  public static final String POSITIONS_INJECTOR_HOST = "positions-injector";
  public static final String POSITIONS_INJECTOR_URI = "/inject";
  public static final String POSITIONS_TRANSPORT_HOST = "positions-transport";
  public static final String POSITIONS_TRANSPORT_URI = "/push";
  public static final String STATIONS_TRANSPORT_HOST = "stations-transport";
  public static final String STATIONS_TRANSPORT_URI = "/push";
  public static final String LISTEN_URI = "/listen";
  public static final String DELAYED_TRAINS_HOST = "delayed-trains";
  public static final String DELAYED_TRAINS_POSITIONS_URI = "/positions";
  public static final String DELAYED_TRAINS_POSITIONS_ADDRESS = "delayed-trains-positions";


}
