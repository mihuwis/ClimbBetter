using ClimbBetter.Api.Features.Difficulties.Endpoints;
using ClimbBetter.Api.Features.TrainingSessions.Endpoints;

namespace ClimbBetter.Api.Features;


public static class EndpointExtensions
{
    public static IEndpointRouteBuilder MapApiEndpoints(
        this IEndpointRouteBuilder app)
    {
        app.MapDifficultyEndpoints();
        app.MapTrainingSessionEndpoints();

        return app;
    }
}