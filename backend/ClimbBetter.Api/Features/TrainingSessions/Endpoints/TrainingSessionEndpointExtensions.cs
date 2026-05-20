using ClimbBetter.Application.CQRS.TrainingSessions.CreateTrainingSession;
using ClimbBetter.Application.CQRS.TrainingSessions.GetTrainingSessions;
using MediatR;

namespace ClimbBetter.Api.Features.TrainingSessions.Endpoints;

public static class TrainingSessionEndpointExtensions
{
    public static IEndpointRouteBuilder MapTrainingSessionEndpoints(
        this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1/training-sessions")
            .WithTags("Training Sessions");

        group.MapPost("/", async (
            CreateTrainingSessionCommand command,
            IMediator mediator,
            CancellationToken cancellationToken) =>
        {
            var id = await mediator.Send(command, cancellationToken);

            return Results.Created($"/api/v1/training-sessions/{id}", id);
        });

        group.MapGet("/", async (
            IMediator mediator,
            CancellationToken cancellationToken) =>
        {
            var result = await mediator.Send(
                new GetTrainingSessionsQuery(),
                cancellationToken);

            return Results.Ok(result);
        });

        return app;
    }
}