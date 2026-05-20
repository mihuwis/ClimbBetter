using ClimbBetter.Application.CQRS.Qualities.GetQualities;
using MediatR;

namespace ClimbBetter.Api.Features.Qualities.Endpoints;

public static class QualityEndpointExtensions
{
    public static IEndpointRouteBuilder MapQualityEndpoints(
        this IEndpointRouteBuilder app
    )
    {
        var group = app.MapGroup("/api/v1/qualities")
        .WithTags("Qualities");

        group.MapGet("/", async (
            IMediator mediator,
            CancellationToken cancellationToken
        ) =>
        {
            var result = await mediator.Send(
                new GetQualitiesQuery(),
                cancellationToken);
            
            return Results.Ok(result);
        });

        return app;
    }
}
