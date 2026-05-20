using ClimbBetter.Application.CQRS.Climbs.GetClimbs;
using MediatR;

namespace ClimbBetter.Api.Features.Climbs.Endpoints;

public static class ClimbEndpointExtensions
{
    public static IEndpointRouteBuilder MapClimbEndpoints(
        this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1/climbs")
            .WithTags("Climbs");

        group.MapGet("/", async (
            IMediator mediator,
            CancellationToken cancellationToken) =>
        {
            var result = await mediator.Send(
                new GetClimbsQuery(),
                cancellationToken);

            return Results.Ok(result);
        });

        return app;
    }
}