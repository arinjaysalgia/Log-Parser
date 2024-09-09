# Flow Log Parser

## Overview

`FlowLogParser` is a Java application designed to parse flow log data, map it to predefined tags using a lookup table, and count occurrences of specific port and protocol combinations. It processes three input files:

- `protocolMapping.csv`: Defines mappings between protocol numbers and protocol names.
- `lookup.csv`: Contains destination port, protocol, and corresponding tags.
- `flowlog.txt`: Contains flow log data that is processed to count tag occurrences and port-protocol combinations.

The program generates two output files:
1. `output.txt`: Contains the count of tags and the count of port-protocol combinations.
2. Error logs (`protocolMapping_error_log.txt`, `lookup_error_log.txt`, and `error_log.txt`): Store invalid entries or errors encountered during processing.

## Features

- **Protocol Mapping**: Reads a CSV file (`protocolMapping.csv`) that maps protocol numbers to protocol names.
- **Tag Lookup**: Reads a CSV lookup table (`lookup.csv`) that maps destination ports and protocols to tags.
- **Flow Log Parsing**: Processes a flow log file (`flowlog.txt`) to count the occurrences of each tag and protocol-port combination.
- **Error Logging**: Logs errors encountered during the reading of the protocol mapping, lookup table, and flow logs into respective error files.
- **Output Generation**: Writes the counts of tags and port-protocol combinations to `output.txt`.

## Prerequisites

- Java JDK 8 or later
- Three required input files:
  - `protocolMapping.csv`: Contains protocol number to name mappings.
  - `lookup.csv`: Contains destination port, protocol, and tag mappings.
  - `flowlog.txt`: Contains flow log entries.

## Assumptions

- This program assumes that any protocol given in the `lookup.csv` and `flowlog.txt` has a valid protocol number and protocol name that are present in the [IANA Protocol Numbers List](https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml).
- The following protocol numbers are not expected to be present in the `lookup.csv` and `flowlog.txt`, as they have no valid mappings:
  - 146-252
  - 253
  - 254
  - 114
  - 99
  - 68
  - 63
  - 61
  
  If any of these protocol numbers are found in the input files, they will not be processed or counted.

- This program has many to many tag to dstport and protocol relationship i.e a give dstport and protocol can map to many tags and the converse of it one tag can be mapped from many dstport and protcol as well 

## Input Files Format

### 1. `protocolMapping.csv`
```csv
ProtocolNumber,ProtocolName
6,TCP
17,UDP
...
```

### 2. `lookup.csv`
```csv
dstport,protocol,tag
80,tcp,web
443,tcp,secure_web
...
```

### 3. `flowlog.txt`
The flow log file should contain space-separated values. The 7th column represents the destination port and the 8th column represents the protocol number.

Example:
```
<other fields> 80 6
<other fields> 443 6
```

## Output Files

### 1. `output.txt`
This file contains two sections:

- **Tag Counts**: The number of occurrences of each tag.
- **Port/Protocol Combination Counts**: The number of occurrences of each destination port and protocol combination.

### 2. Error Logs
- `protocolMapping_error_log.txt`: Logs any errors related to invalid lines in the `protocolMapping.csv` file.
- `lookup_error_log.txt`: Logs errors related to invalid lines in the `lookup.csv` file.
- `error_log.txt`: Logs errors encountered while processing `flowlog.txt`.



## How to Run

1. Ensure you have the required input files (`protocolMapping.csv`, `lookup.csv`, `flowlog.txt`) in the same directory as the program.
2. Compile and run the program:

```bash
javac FlowLogParser.java
java FlowLogParser
```

3. Upon execution, the program will process the input files and generate `output.txt` and error log files (if any errors occur).

## Error Handling

- Invalid lines in the `protocolMapping.csv` or `lookup.csv` files will be logged into the respective error log files and skipped.
- Invalid flow log entries will be logged into `error_log.txt`.
  
## License

This project is licensed under the MIT License.

## Contact

For issues or questions, please contact the project owner.