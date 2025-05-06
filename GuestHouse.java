import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

public class GuestHouse implements Serializable {
    private String roomName;
    private String area;
    private int noOfPersons;
    private int stars;
    private int noOfReviews;
    private String roomImage;
    private ArrayList<Date> availableDates;
    private int pricePerNight;

    // Constructor
    public GuestHouse(String roomName, String area, int noOfPersons, int stars, int noOfReviews, String roomImage, ArrayList<Date> availableDates, int pricePerNight) {
        this.roomName = roomName;
        this.area = area;
        this.noOfPersons = noOfPersons;
        this.stars = stars;
        this.noOfReviews = noOfReviews;
        this.roomImage = roomImage;
        this.availableDates = availableDates;
        this.pricePerNight = pricePerNight;
    }

    // Getters and Setters
    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public int getNoOfPersons() {
        return noOfPersons;
    }

    public void setNoOfPersons(int noOfPersons) {
        this.noOfPersons = noOfPersons;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getNoOfReviews() {
        return noOfReviews;
    }

    public void setNoOfReviews(int noOfReviews) {
        this.noOfReviews = noOfReviews;
    }

    public String getRoomImage() {
        return roomImage;
    }

    public void setRoomImage(String roomImage) {
        this.roomImage = roomImage;
    }

    public ArrayList<Date> getAvailableDates() {
        return availableDates;
    }

    public void setAvailableDates(ArrayList<Date> availableDates) {
        this.availableDates = availableDates;
    }

    public int getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(int pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String datesFormatted = availableDates.stream()
                                .map(date -> dateFormat.format(date))
                                .collect(Collectors.joining(", "));
        return "GuestHouse{" +
                "roomName='" + roomName + '\'' +
                ", area='" + area + '\'' +
                ", noOfPersons=" + noOfPersons +
                ", stars=" + stars +
                ", noOfReviews=" + noOfReviews +
                ", roomImage='" + roomImage + '\'' +
                ", availableDates=[" + datesFormatted + "]" +
                ", pricePerNight=" + pricePerNight +
                '}';
    }
}




