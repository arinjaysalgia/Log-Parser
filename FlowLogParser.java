import java.io.*;
import java.util.*;

public class FlowLogParser {

   
    public static Map<String, String> ProtocolNumberToName;
    public static Map<String, String> ProtocolNameToNumber;

    public static void main(String[] args) {
        
        BufferedWriter lookupErrorWriter = null;
        BufferedWriter parseErrorWriter = null;
        BufferedWriter protocolErrorWriter = null;
        ProtocolNameToNumber = new HashMap<>(150);
        ProtocolNumberToName = new HashMap<>(150);

        try {
            // Check if the required files are present
            File protocolMappingFile = new File("protocolMapping.csv");
            File lookupFile = new File("lookup.csv");
            File flowLogFile = new File("flowlog.txt");
    
            if (!protocolMappingFile.exists()) {
                System.err.println("Error: protocol Mapping file 'protocolMapp.csv' not found.");
                return;
            }
            if (!lookupFile.exists()) {
                System.err.println("Error: Lookup file 'lookup.csv' not found.");
                return;
            }
            if (!flowLogFile.exists()) {
                System.err.println("Error: Flow log file 'flowlog.txt' not found.");
                return;
            }

            // Read Protocol Mapping Table
            protocolErrorWriter = new BufferedWriter(new FileWriter("protocolMapping_error_log.txt"));
            readProtocolMapping(protocolMappingFile.getPath(),protocolErrorWriter);

            // Read lookup table
            lookupErrorWriter = new BufferedWriter(new FileWriter("lookup_error_log.txt"));
            Map<String, List<String>> lookUpMap = readLookupTable(lookupFile.getPath(),lookupErrorWriter);

            // Process flow log file
            Map<String, Integer> tagCounts = new HashMap<>();
            Map<String, Integer> portProtocolCounts = new HashMap<>();
            parseErrorWriter = new BufferedWriter(new FileWriter("error_log.txt"));
            processFlowLog(flowLogFile.getPath(), lookUpMap, tagCounts, portProtocolCounts, parseErrorWriter);

            // Write output to a file
            writeOutput("output.txt", tagCounts, portProtocolCounts);

        } catch (IOException e) {
            System.err.println("An error occurred while processing the files: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (protocolErrorWriter != null) {
                    protocolErrorWriter.close(); 
                }

                if (lookupErrorWriter != null) {
                    lookupErrorWriter.close(); 
                }

                if (parseErrorWriter != null) {
                    parseErrorWriter.close(); 
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Function to read the protocol Mapping from a CSV file
    public static void readProtocolMapping(String fileName, BufferedWriter errorWriter) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
    
            while ((line = reader.readLine()) != null) {
                
                if (line.trim().isEmpty() || line.startsWith("ProtocolNumber")) {
                    continue;
                }
                String[] parts = line.split(",");
                
                if (parts.length != 2) {
                    errorWriter.write("Invalid line in ProtocolMappingFile file: " + line + "\n");
                    continue; 
                }
                String protocolNumber = parts[0].trim();
                String protocolName = parts[1].trim().toLowerCase();
                ProtocolNumberToName.put(protocolNumber, protocolName);
                ProtocolNameToNumber.put(protocolName,protocolNumber);
            }
        }
    }

    // Function to read the lookup table from a CSV file
    public static Map<String, List<String>> readLookupTable(String fileName, BufferedWriter errorWriter) throws IOException {
        Map<String, List<String>> lookUpMap = new HashMap<>(10000);
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
    
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("dstport")) {
                    continue; // Skip headers or empty lines
                }
                String[] parts = line.split(",");
                
                if (parts.length != 3) {
                    errorWriter.write("Invalid line in lookup file: " + line + "\n");
                    continue; // Skip invalid lines
                }
                
                String dstPort= parts[0].trim();
                String protocol = getProtocolNumber(parts[1].trim().toLowerCase());
                String tag = parts[2].trim();
                String keyEntry = protocol + "-" + dstPort;
                List<String> tagList = lookUpMap.getOrDefault(keyEntry, new LinkedList<>());
                tagList.add(tag);
                lookUpMap.put(keyEntry, tagList);
            }
        }
        return lookUpMap;
    }

    public static String getProtocolNumber(String protocolName){
        return ProtocolNameToNumber.get(protocolName);

    }

    public  static String getProtocolName(String protocolNumber) {
        return ProtocolNumberToName.get(protocolNumber);
    }

    public static void processFlowLog(String fileName, Map<String, List<String>> lookUpMap,  Map<String, Integer> tagCounts, 
                                        Map<String, Integer> portProtocolCounts, BufferedWriter parseErrorWriter) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) { 
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length < 7) {
                    parseErrorWriter.write("Invalid flow log entry: " + line + "\n");
                    continue;
                }

                String dstPort = parts[6].trim();
                if(!isValiddstPort(dstPort, parseErrorWriter, line)){
                    continue;
                }
                
                String protocolNumber = parts[7].trim();                 
                if(!isValidProtocol(protocolNumber, parseErrorWriter, line)){
                    continue;
                }
                String keyEntry = protocolNumber + "-" + dstPort; 
                
                if(lookUpMap.containsKey(keyEntry)){
                    List<String> tagList = lookUpMap.get(keyEntry);
                    for(String mapTag : tagList){
                        tagCounts.put(mapTag, tagCounts.getOrDefault(mapTag, 0) + 1);
                    }
                }
                else{
                    String tag = "Untagged";
                    tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);    
                }

                // Update port/protocol combination counts
                String portAndProtocol = dstPort + "," + getProtocolName(protocolNumber);
                portProtocolCounts.put(portAndProtocol, portProtocolCounts.getOrDefault(portAndProtocol, 0) + 1);
            }
        } 
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static boolean isValiddstPort(String dstPort, BufferedWriter parseErrorWriter, String line){

        try {
            int port = Integer.parseInt(dstPort);
            if (port >= 0 && port <= 65536) {
                return true;
            } else {
                parseErrorWriter.write("Invalid flow log dstPortNumber and not in between 0 to 65536 : " + line + "\n");
                return false;
            }
        } catch (NumberFormatException e) {
            try {
                parseErrorWriter.write("Invalid flow log dstPortNumber as not a number : " + line + "\n");
            } 
            catch (IOException ex) {
                System.err.println("Unable to write error log ");
            }
            return false;
        } 
        catch (IOException ex) {
            System.err.println("Unable to write error log ");
            return false;
        }
    }
    
    public static boolean isValidProtocol(String protocolNumber, BufferedWriter parseErrorWriter, String line){

        try {
            int protocol = Integer.parseInt(protocolNumber);
            if (protocol >= 0 && protocol <= 256) {
                return true;
            } else {
                parseErrorWriter.write("Invalid flow log protocolNumber and not in between 0 to 256 : " + line + "\n");
                return false;
            }
        } catch (NumberFormatException e) {
            try {
                parseErrorWriter.write("Invalid flow log protocolNumber as not a number : " + line + "\n");
            } 
            catch (IOException ex) {
                System.err.println("Unable to write error log ");
            }
            return false;
        } 
        catch (IOException ex) {
            System.err.println("Unable to write error log ");
            return false;
        }
    }

    // Function to write the results to an output file
    public static void writeOutput(String fileName, Map<String, Integer> tagCounts, 
                                   Map<String, Integer> portProtocolCounts) throws IOException {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(fileName));

            // Write Tag Counts
            writer.write("Tag Counts:\n");
            writer.write("Tag,Count\n");
            for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }

            // Write Port/Protocol Combination Counts
            writer.write("\nPort/Protocol Combination Counts:\n");
            writer.write("Port,Protocol,Count\n");
            for (Map.Entry<String, Integer> entry : portProtocolCounts.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }

        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
