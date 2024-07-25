#!/bin/bash

# Initialize variables
reports_dir=""
output_file=""

# Function to show usage
usage() {
    echo "Usage: $0 -d <reports_dir> -o <output_file>"
    exit 1
}

# Parse command-line options
while getopts "d:o:" opt; do
    case $opt in
        d) reports_dir=$OPTARG ;;
        o) output_file=$OPTARG ;;
        ?) usage ;;
    esac
done

# Check if both arguments were provided
if [ -z "$reports_dir" ] || [ -z "$output_file" ]; then
    usage
fi

# Write XML header to the output file
echo '<?xml version="1.0" encoding="UTF-8"?>' > "$output_file"
echo '<testsuites>' >> "$output_file"

# Merge the content of all input JUnit files in the directory.
# (Note that in the case of Unit Tests, the JUnit XML files produced by Gradle
# don't have a parent `<testsuites>` root tag, so there's no need to try and remove it)
sed '/<\?xml .*\?>/d' "$reports_dir"/*.xml >> "$output_file"

# Close the testsuites tag
echo '</testsuites>' >> "$output_file"

# Print the result
echo "Merged XML reports into $output_file"
