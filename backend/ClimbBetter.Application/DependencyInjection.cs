
using ClimbBetter.Application.CQRS.TrainingSessions.CreateTrainingSession;
using Microsoft.Extensions.DependencyInjection;


namespace ClimbBetter.Application;

public static class DependencyInjection
{
    public static IServiceCollection AddApplication(
        this IServiceCollection services)
    {
        services.AddMediatR(
            cfg => 
            cfg.RegisterServicesFromAssemblyContaining<CreateTrainingSessionCommand>());

            return services;
    }
}