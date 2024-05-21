#!/bin/bash

# Check if the number of arguments provided is correct
if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <number_of_servers>"
  exit 1
fi

# Array to store the PIDs of the servers
pids=()

# Extract the number of servers from the command-line argument
N=$1

# Start servers with increasing port numbers and qualifier
for ((i = 0; i < N; i++)); do
  port=$((7300 + i))  # Starting port number 6000
  qualifier=$(printf "\\$(printf '%03o' $((i + 65)))")  # Convert index to corresponding letter (A=65, B=66, ...)
  
  # Invoke mvn command to start the server in the background and store its PID
  mvn clean compile exec:java -Dexec.args="localhost $port $qualifier TupleSpaces" &
  
  # Store the PID of the last background process started
  pids+=($!)
  
  # Sleep for a short duration to allow each server to start before starting the next one
  sleep 6
done


# Function to kill all servers
kill_servers() {
  echo -e "\nKilling all servers..."

  for pid in "${pids[@]}"; do
    # Send SIGINT signal to the process
    kill $pid
    # Wait for the process to terminate
    wait $pid
  done
}

# Trap SIGINT (Ctrl+C) to kill servers before exiting
trap 'kill_servers; exit' SIGINT

# Wait for background processes to finish
wait
