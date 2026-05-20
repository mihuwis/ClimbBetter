using MediatR;

namespace ClimbBetter.Application.CQRS.Climbs.GetClimbs;

public record GetClimbsQuery : IRequest<List<ClimbDto>>;