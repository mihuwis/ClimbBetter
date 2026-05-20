using ClimbBetter.Application.CQRS.TrainingEntries.AddTrainingEntry;
using MediatR;

namespace ClimbBetter.Api.Features.TrainingEntries.Endpoints;

public static class TrainingEntryEndpointExtensions
{
    public static IEndpointRouteBuilder MapTrainingEntryEndpoints(
        this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1/training-sessions")
            .WithTags("Training Entries");

        group.MapPost("/{sessionId:guid}/entries", async (
            Guid sessionId,
            AddTrainingEntryCommand command,
            IMediator mediator,
            CancellationToken cancellationToken) =>
        {
            command.TrainingSessionId = sessionId;

            var id = await mediator.Send(command, cancellationToken);

            return Results.Created(
                $"/api/v1/training-sessions/{sessionId}/entries/{id}",
                id);
        });

        return app;
    }
}