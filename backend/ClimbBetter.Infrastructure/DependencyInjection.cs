using ClimbBetter.Domain.Interfaces;
using ClimbBetter.Infrastructure.Persistence;
using ClimbBetter.Infrastructure.Repositories;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
namespace ClimbBetter.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("DefaultConnection");

        services.AddDbContext<ClimbBetterDbContext>(options => 
            options.UseNpgsql(connectionString));

        services.AddScoped<IDifficultyRepository, DifficultyRepository>();
        services.AddScoped<IQualityRepository, QualityRepository>();
        services.AddScoped<IClimbRepository, ClimbRepository>();
        services.AddScoped<ITrainingSessionRepository, TrainingSessionRepository>();


        return services;
    }
}