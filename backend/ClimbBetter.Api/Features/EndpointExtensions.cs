using ClimbBetter.Api.Features.Difficulties.Endpoints;
using ClimbBetter.Api.Features.TrainingSessions.Endpoints;
using ClimbBetter.Api.Features.Qualities.Endpoints;
using ClimbBetter.Api.Features.Climbs.Endpoints;
using ClimbBetter.Api.Features.TrainingEntries.Endpoints;

namespace ClimbBetter.Api.Features;


public static class EndpointExtensions
{
    public static IEndpointRouteBuilder MapApiEndpoints(
        this IEndpointRouteBuilder app)
    {
        app.MapDifficultyEndpoints();
        app.MapTrainingSessionEndpoints();
        app.MapQualityEndpoints();
        app.MapClimbEndpoints();
        app.MapTrainingEntryEndpoints();


        return app;
    }
}