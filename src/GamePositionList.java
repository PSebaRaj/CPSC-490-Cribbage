import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamePositionList {

    Map<GamePosition, Double> positionValues;

    public GamePositionList(Map<GamePosition, Double> positionValues) {
        this.positionValues = positionValues;
    }

    public GamePositionList() {
        this.positionValues = new HashMap<>();
    }
    public void saveToFile(String fileName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(this.positionValues);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readFromFile(String fileName) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            this.positionValues = (HashMap<GamePosition, Double>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void add(GamePosition position, double value) {
        positionValues.put(position, value);
    }

    public double get(GamePosition position) {
        return positionValues.get(position);
    }

    public List<GamePosition> getPositions() {
        return new ArrayList<>(positionValues.keySet());
    }

    @Override
    public String toString() {
        return positionValues.toString();
    }

}
