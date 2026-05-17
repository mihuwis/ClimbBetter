using ClimbBetter.Application.CQRS.Difficulties.GetDifficulties;
using MediatR;

namespace ClimbBetter.Api.Features.Difficulties.Endpoints;

public static class DifficultyEndpointExtensions
{
    public static IEndpointRouteBuilder MapDifficultyEndpoints(
        this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1/difficulties")
            .WithTags("Difficulties");

        group.MapGet("/", async (
            IMediator mediator,
            CancellationToken cancellationToken) =>
        {
            var result = await mediator.Send(
                new GetDifficultiesQuery(),
                cancellationToken);

            return Results.Ok(result);
        });

        return app;
    }
}