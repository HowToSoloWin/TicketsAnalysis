import java.io.FileReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.google.gson.*;

public class TicketsAnalysis {

    static class Ticket {
        String origin;
        String destination;
        String departure_date;
        String departure_time;
        String arrival_date;
        String arrival_time;
        String carrier;
        int price;

        LocalDateTime getDepartureDateTime() {
    return LocalDateTime.parse(departure_date + " " + departure_time, DateTimeFormatter.ofPattern("dd.MM.yy H:mm"));
    }

LocalDateTime getArrivalDateTime() {
    return LocalDateTime.parse(arrival_date + " " + arrival_time, DateTimeFormatter.ofPattern("dd.MM.yy H:mm"));
    }


        long getFlightDurationMinutes() {
            Duration duration = Duration.between(getDepartureDateTime(), getArrivalDateTime());
            return duration.toMinutes();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java TicketsAnalysis <tickets.json>");
            System.exit(1);
        }

        String filename = args[0];

        // Читаем и парсим JSON
        JsonObject root = JsonParser.parseReader(new FileReader(filename)).getAsJsonObject();
        JsonArray ticketsJson = root.getAsJsonArray("tickets");

        List<Ticket> tickets = new ArrayList<>();

        Gson gson = new Gson();
        for (JsonElement je : ticketsJson) {
            Ticket t = gson.fromJson(je, Ticket.class);
            // Фильтрация маршрута Владивосток - Тель-Авив
            if ("VVO".equals(t.origin) && "TLV".equals(t.destination)) {
                tickets.add(t);
            }
        }

        if (tickets.isEmpty()) {
            System.out.println("Нет билетов между Владивостоком и Тель-Авивом.");
            return;
        }

        // Группируем билеты по перевозчику
        Map<String, List<Ticket>> ticketsByCarrier = new HashMap<>();
        for (Ticket t : tickets) {
            ticketsByCarrier.computeIfAbsent(t.carrier, k -> new ArrayList<>()).add(t);
        }

        // Минимальное время полета по перевозчикам
        System.out.println("Минимальное время полета (в минутах) между Владивостоком и Тель-Авивом для каждого перевозчика:");
        for (Map.Entry<String, List<Ticket>> entry : ticketsByCarrier.entrySet()) {
            String carrier = entry.getKey();
            List<Ticket> carrierTickets = entry.getValue();

            long minDuration = carrierTickets.stream()
                    .mapToLong(Ticket::getFlightDurationMinutes)
                    .min()
                    .orElse(-1);

            System.out.printf("%s: %d минут%n", carrier, minDuration);
        }

        // Средняя цена и медиана по всем билетам
        List<Integer> prices = new ArrayList<>();
        for (Ticket t : tickets) {
            prices.add(t.price);
        }
        Collections.sort(prices);

        double average = prices.stream().mapToInt(Integer::intValue).average().orElse(0);
        double median;
        int n = prices.size();
        if (n % 2 == 1) {
            median = prices.get(n / 2);
        } else {
            median = (prices.get(n / 2 - 1) + prices.get(n / 2)) / 2.0;
        }

        System.out.printf("Средняя цена: %.2f%n", average);
        System.out.printf("Медиана цены: %.2f%n", median);
        System.out.printf("Разница между средней ценой и медианой: %.2f%n", average - median);
    }
}
