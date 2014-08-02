param($version)


echo "Executing script"

echo "Version passed in is $version"

$args | %{ write-host "Argument:" $_}
