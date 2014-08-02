param($version)


echo "Executing script"

echo "Version passed in is $version"

write-host "Is 64 bit:" $([Environment]::Is64BitProcess)

$args | %{ write-host "Argument:" $_}
