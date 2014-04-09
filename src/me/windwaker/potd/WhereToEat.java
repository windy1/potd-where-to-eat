package me.windwaker.potd;

import me.windwaker.places.GooglePlaces;
import me.windwaker.places.Place;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static me.windwaker.places.GooglePlaces.MAXIMUM_RESULTS;
import static me.windwaker.places.GooglePlaces.Param;

public class WhereToEat {
    private static final String API_KEY = "********";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // initialize client
        GooglePlaces client = new GooglePlaces(API_KEY);
        client.setSensorEnabled(false);

        while (true) {
            // get zip code
            System.out.print("Zip code: ");
            String zip = scanner.next();

            // get lat lng of zip
            double lat = 0, lng = 0;
            try {
                String url = String.format("https://maps.googleapis.com/maps/api/geocode/json?components=postal_code:%s" +
                        "&sensor=false&key=%s", zip, API_KEY);
                HttpGet get = new HttpGet(url);
                String rawJson = readString(client.getHttpClient().execute(get));
                JSONObject json = new JSONObject(rawJson).getJSONArray("results").getJSONObject(0).getJSONObject("geometry")
                        .getJSONObject("location");
                lat = json.getDouble("lat");
                lng = json.getDouble("lng");
            } catch (URISyntaxException | IOException | HttpException e) {
                e.printStackTrace();
            }
            System.out.printf("Latitude/Longitude: %f, %f\n", lat, lng);

            // input
            System.out.print("Radius (m): ");
            double radius = scanner.nextDouble();
            System.out.print("Minimum price (?/4): ");
            int minPrice = scanner.nextInt();
            System.out.print("Maximum price (?/4): ");
            int maxPrice = scanner.nextInt();

            // get nearby places
            List<Place> places =
                    client.getNearbyPlaces(lat, lng, radius, MAXIMUM_RESULTS, Param.name("types").value("restaurant"),
                            Param.name("minprice").value(minPrice), Param.name("maxprice").value(maxPrice));

            if (places.isEmpty()) {
                System.out.println("No places found.");
                break;
            }

            Random random = new Random();
            Place place = places.get(random.nextInt(places.size()));

            // dont pick a place twice
            try {
                Scanner fs = new Scanner(new FileReader("last.place"));
                if (fs.hasNext()) {
                    String id = fs.next();
                    while (id.equals(place.getId())) {
                        place = places.get(random.nextInt(places.size()));
                    }
                }
                fs.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // print the details
            place = place.getDetails();
            System.out.println("\nYou should eat at: " + place.getName());
            System.out.println("Address: " + place.getAddress());
            System.out.println("Phone #: " + place.getPhoneNumber());
            System.out.println("Rating: " + place.getRating() + "/5");
            System.out.println("Price: " + place.getPrice());

            // write the id to disk
            try {
                PrintWriter writer = new PrintWriter(new FileWriter("last.place"));
                writer.println(place.getId());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean end = false;
            while (true) {
                System.out.print("\nTry again? (y/n): ");
                String next = scanner.next();
                if (next.equalsIgnoreCase("y")) {
                    break;
                } else if (next.equalsIgnoreCase("n")) {
                    end = true;
                    break;
                }
            }

            if (end) {
                break;
            }
        }
        scanner.close();
    }

    private static String readString(HttpResponse response) throws IOException {
        return IOUtils.toString(response.getEntity().getContent());
    }
}
