# DayQuest Docker Management Script for Windows
# Usage: .\docker.ps1 [command]
# Commands: dev, prod, down, build, logs, clean

param(
    [Parameter(Position=0)]
    [ValidateSet("dev", "prod", "down", "build", "logs", "clean", "help")]
    [string]$Command = "help"
)

$DEV_COMPOSE_FILE = "docker-compose.dev.yml"
$PROD_COMPOSE_FILE = "docker-compose.yml"
$ENV_FILE = "stack.env"

function Show-Banner {
    Write-Host " ____  _____ __ __ _____ _____ _____ _____ _____     " -ForegroundColor Cyan
    Write-Host "|    \|  _  |  |  |     |  |  |   __|   __|_   _|    " -ForegroundColor Cyan
    Write-Host "|  |  |     |_   _|  |  |  |  |   __|__   | | |      " -ForegroundColor Cyan
    Write-Host "|____/|__|__| |_| |__  _|_____|_____|_____| |_|      " -ForegroundColor Cyan
    Write-Host "                     |__|               by AgentP    " -ForegroundColor Cyan
    Write-Host ""
}

function Show-Help {
    Show-Banner
    Write-Host "DayQuest Docker Management Script" -ForegroundColor Green
    Write-Host ""
    Write-Host "Usage: .\docker.ps1 [command]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Commands:"
    Write-Host "  dev     - Run Docker Compose in development mode"
    Write-Host "  prod    - Run Docker Compose in production mode"
    Write-Host "  down    - Stop and remove containers"
    Write-Host "  build   - Build the containers"
    Write-Host "  logs    - Tail logs of the containers"
    Write-Host "  clean   - Clean up Docker resources"
    Write-Host "  help    - Show this help message"
}

switch ($Command) {
    "dev" {
        Show-Banner
        Write-Host "Starting Docker Compose in development mode..." -ForegroundColor Green
        docker-compose --env-file $ENV_FILE -f $DEV_COMPOSE_FILE up --build
    }
    "prod" {
        Show-Banner
        Write-Host "Starting Docker Compose in production mode..." -ForegroundColor Green
        docker-compose --env-file $ENV_FILE -f $PROD_COMPOSE_FILE up --build
    }
    "down" {
        Write-Host "Stopping and removing containers..." -ForegroundColor Yellow
        docker-compose --env-file $ENV_FILE -f $DEV_COMPOSE_FILE down
        docker-compose --env-file $ENV_FILE -f $PROD_COMPOSE_FILE down
    }
    "build" {
        Show-Banner
        Write-Host "Building Docker images..." -ForegroundColor Green
        docker-compose --env-file $ENV_FILE -f $DEV_COMPOSE_FILE build
    }
    "logs" {
        Write-Host "Tailing logs of containers..." -ForegroundColor Green
        docker-compose --env-file $ENV_FILE -f $DEV_COMPOSE_FILE logs -f
    }
    "clean" {
        Write-Host "Cleaning up Docker resources..." -ForegroundColor Red
        docker-compose --env-file $ENV_FILE -f $DEV_COMPOSE_FILE down -v --rmi local
        docker system prune -f
    }
    "help" {
        Show-Help
    }
}

