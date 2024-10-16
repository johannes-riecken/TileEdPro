# TileEdPro

## Description

The purpose of the `TileEdPro` application is to provide a simple tile map editor for creating and managing tile-based maps. The application allows users to create, edit, and save tile maps using a graphical user interface. Key features of the application include:

* Creating new tile maps with customizable dimensions and tile sets
* Loading and saving tile maps from and to files
* Adding, removing, and editing tiles on the map
* Supporting multiple layers for organizing tiles
* Providing tools for flipping, rotating, and moving tiles
* Exporting tile maps as images
* Customizing the background color of the tile map

The application is built using Java and the Swing Application Framework, and it leverages the `tilelib` library for handling tile maps and tile sets. The main components of the application are located in the `TileApp/src/main/java/tileedpro` directory, with additional resources and configuration files in the `TileApp/src/main/java/tileedpro/resources` directory. The main entry point of the application is the `TileEdProApp` class in the `TileApp/src/main/java/tileedpro/TileEdProApp.java` file.

## Installation

To set up the project locally, follow these steps:

1. Clone the repository:
   ```sh
   git clone https://github.com/johannes-riecken/TileEdPro.git
   cd TileEdPro
   ```

2. Build the project using Maven:
   ```sh
   mvn clean install
   ```

## Usage

To run the `TileEdPro` application, use the following command:
```sh
mvn exec:java -Dexec.mainClass="tileedpro.TileEdProApp"
```

### Creating a New Tile Map

1. Open the application.
2. Go to `File` > `New Tilemap`.
3. Select a tileset and configure the dimensions of the new tile map.
4. Click `OK` to create the new tile map.

### Editing a Tile Map

1. Select a tile from the tileset panel.
2. Click on the tile map to place the selected tile.
3. Use the toolbar buttons to flip, rotate, or move tiles as needed.

### Saving a Tile Map

1. Go to `File` > `Save Tilemap` to save the current tile map.
2. To save the tile map with a different name, go to `File` > `Save Tilemap As...`.

## Contributing

We welcome contributions to the `TileEdPro` project! To contribute, follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Make your changes and commit them with descriptive messages.
4. Push your changes to your forked repository.
5. Create a pull request to the main repository.

Please ensure that your code follows the project's coding standards and includes appropriate tests.

## License

This project is licensed under the MIT License. See the `LICENSE` file for more information.
