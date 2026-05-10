param(
    [Parameter(Mandatory = $true)]
    [int]$BranchId,

    [string]$BaseUrl = "http://localhost:8080",

    [int]$DoctorId,

    [string]$Date = ((Get-Date).Date.AddDays(1).ToString("yyyy-MM-dd"))
)

$ErrorActionPreference = "Stop"

function Invoke-ApiGet {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    Invoke-RestMethod -Method Get -Uri $Url -ContentType "application/json"
}

$base = $BaseUrl.TrimEnd("/")
$doctorsUrl = "$base/api/v1/users/branch/$BranchId/doctors"

Write-Host "== Doctores de la sucursal $BranchId ==" -ForegroundColor Cyan
try {
    $doctors = Invoke-ApiGet -Url $doctorsUrl
    if (-not $doctors -or $doctors.Count -eq 0) {
        Write-Host "No hay doctores disponibles." -ForegroundColor Yellow
    } else {
        $doctors |
            Select-Object id, fullName, email, phone, role |
            Format-Table -AutoSize
    }
}
catch {
    Write-Host "Error consultando doctores: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

if ($PSBoundParameters.ContainsKey('DoctorId')) {
    Write-Host "`n== Slots disponibles ==" -ForegroundColor Cyan
    $slotsUrl = "$base/api/v1/appointments/available-slots?branchId=$BranchId&doctorId=$DoctorId&date=$Date"
    try {
        $slots = Invoke-ApiGet -Url $slotsUrl
        if (-not $slots -or $slots.Count -eq 0) {
            Write-Host "No hay slots disponibles para el doctor $DoctorId en la fecha $Date." -ForegroundColor Yellow
        } else {
            $slots |
                Select-Object start, end |
                Format-Table -AutoSize
        }
    }
    catch {
        Write-Host "Error consultando slots: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "`nTip: para probar slots agrega -DoctorId <id> -Date yyyy-MM-dd" -ForegroundColor DarkGray
}

