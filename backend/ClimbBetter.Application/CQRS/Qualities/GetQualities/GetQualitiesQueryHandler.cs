using ClimbBetter.Domain.Interfaces;
using MediatR;

namespace ClimbBetter.Application.CQRS.Qualities.GetQualities;

public class GetQualitiesQueryHandler
    : IRequestHandler<GetQualitiesQuery, List<QualityDto>>
{
    private readonly IQualityRepository _qualityRepository;

    public GetQualitiesQueryHandler(IQualityRepository qualityRepository)
    {
        _qualityRepository = qualityRepository;
    }

    public async Task<List<QualityDto>> Handle(
        GetQualitiesQuery request,
        CancellationToken cancellationToken)
    {
        var qualities = await _qualityRepository.GetAllAsync(cancellationToken);

        return qualities
            .Select(q => new QualityDto
            {
                Id = q.Id,
                Name = q.Name,
                Code = q.Code,
                Multiplier = q.Multiplier
            })
            .ToList();
    }
}