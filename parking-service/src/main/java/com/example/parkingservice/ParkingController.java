package com.example.parkingservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/parking")
public class ParkingController {

    @Value("${server.port}")
    private String serverPort;

    // The shared database file both JVMs will read and write to
    private static final String DB_FILE = "shared_parking_db.dat";

    static class ParkingSlot implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id, name, zone;
        public double hourlyRate;
        public boolean isAvailable;
        public String driverName, vehiclePlate, vehicleType;
        public Long entryTime;

        public ParkingSlot(String id, String name, String zone, double hourlyRate) {
            this.id = id; this.name = name; this.zone = zone; this.hourlyRate = hourlyRate;
            this.isAvailable = true;
        }
    }

    static class BookingRequest { 
        public String driverName; 
        public String vehiclePlate; 
        public String vehicleType; 
    }

    // --- SHARED DATABASE LOGIC ---
    @SuppressWarnings("unchecked")
    private synchronized List<ParkingSlot> getDbSlots() {
        File file = new File(DB_FILE);
        if (!file.exists()) {
            List<ParkingSlot> defaultSlots = new ArrayList<>(Arrays.asList(
                    new ParkingSlot("1", "Slot A1", "City Center Zone", 5.0),
                    new ParkingSlot("2", "Slot B2", "City Center Zone", 5.0),
                    new ParkingSlot("3", "Slot C3", "VIP Transit Hub", 12.0)
            ));
            saveDbSlots(defaultSlots);
            return defaultSlots;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<ParkingSlot>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private synchronized void saveDbSlots(List<ParkingSlot> slots) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DB_FILE))) {
            oos.writeObject(slots);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ----------------------------

    @GetMapping("/slots")
    public Map<String, Object> getSlots() {
        return Map.of("handledByPort", serverPort, "slots", getDbSlots());
    }

    @PostMapping("/slots")
    public Map<String, Object> createSlot(@RequestBody ParkingSlot newSlot) {
        List<ParkingSlot> slots = getDbSlots();
        newSlot.isAvailable = true;
        slots.add(newSlot);
        saveDbSlots(slots);
        return Map.of("handledByPort", serverPort, "message", "Slot Created", "slot", newSlot);
    }

    @PutMapping("/slots/{id}")
    public Map<String, Object> updateSlot(@PathVariable String id, @RequestBody ParkingSlot updatedSlot) {
        List<ParkingSlot> slots = getDbSlots();
        for (ParkingSlot slot : slots) {
            if (slot.id.equals(id)) {
                slot.name = updatedSlot.name; slot.zone = updatedSlot.zone; slot.hourlyRate = updatedSlot.hourlyRate;
                saveDbSlots(slots);
                return Map.of("handledByPort", serverPort, "message", "Slot Updated", "slot", slot);
            }
        }
        return Map.of("handledByPort", serverPort, "error", "Slot not found");
    }

    @DeleteMapping("/slots/{id}")
    public Map<String, Object> deleteSlot(@PathVariable String id) {
        List<ParkingSlot> slots = getDbSlots();
        slots.removeIf(slot -> slot.id.equals(id));
        saveDbSlots(slots);
        return Map.of("handledByPort", serverPort, "message", "Slot Deleted");
    }

    @PostMapping("/book/{id}")
    public Map<String, Object> bookSlot(@PathVariable String id, @RequestBody BookingRequest req) {
        List<ParkingSlot> slots = getDbSlots();
        for (ParkingSlot slot : slots) {
            if (slot.id.equals(id) && slot.isAvailable) {
                slot.isAvailable = false; 
                slot.driverName = req.driverName;
                slot.vehiclePlate = req.vehiclePlate; 
                slot.vehicleType = req.vehicleType; 
                slot.entryTime = System.currentTimeMillis();
                saveDbSlots(slots);
                return Map.of("handledByPort", serverPort, "message", "Successfully assigned " + slot.name);
            }
        }
        return Map.of("handledByPort", serverPort, "error", "Slot already occupied or not found.");
    }

    @PostMapping("/leave/{id}")
    public Map<String, Object> leaveSlot(@PathVariable String id) {
        List<ParkingSlot> slots = getDbSlots();
        for (ParkingSlot slot : slots) {
            if (slot.id.equals(id) && !slot.isAvailable) {
                long exitTime = System.currentTimeMillis();
                double simulatedHours = (exitTime - slot.entryTime) / 10000.0;
                double cost = simulatedHours * slot.hourlyRate;
                
                if ("EV".equalsIgnoreCase(slot.vehicleType)) cost *= 0.8;
                
                String receiptMsg = String.format("Vehicle %s checked out. Duration: %.1f simulated hrs. Total: $%.2f", 
                                                  slot.vehiclePlate, simulatedHours, cost);
                
                slot.isAvailable = true; slot.driverName = null; slot.vehiclePlate = null; 
                slot.vehicleType = null; slot.entryTime = null;
                saveDbSlots(slots);
                return Map.of("handledByPort", serverPort, "message", receiptMsg, "cost", cost);
            }
        }
        return Map.of("handledByPort", serverPort, "error", "Slot empty");
    }
}
