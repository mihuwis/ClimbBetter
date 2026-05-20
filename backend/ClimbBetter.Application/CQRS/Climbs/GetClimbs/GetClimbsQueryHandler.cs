
using ClimbBetter.Domain.Interfaces;
using MediatR;

namespace ClimbBetter.Application.CQRS.Climbs.GetClimbs;

using System.Threading;
using System.Threading.Tasks;
using MediatR;

public class GetClimbsQueryHandler : 
    IRequestHandler<GetClimbsQuery, List<ClimbDto>>
{
    private readonly IClimbRepository _climbRepository;

    public GetClimbsQueryHandler(IClimbRepository climbRepository)
    {
        _climbRepository = climbRepository;
    }

    public async Task<List<ClimbDto>> Handle(GetClimbsQuery request, CancellationToken cancellationToken)
    {
        var climbs = await _climbRepository.GetAllAsync(cancellationToken);

        return climbs
            .Select(c => new ClimbDto
            {
                Id = c.Id,
                Name = c.Name,
                Type = c.Type,
                LocationId = c.LocationId,
                DifficultyId = c.DifficultyId,
                LengthMeters = c.LengthMeters,
                SuggestedMoveCount = c.SuggestedMoveCount,
                IsPublic = c.IsPublic
            })
            .ToList();
    }
}